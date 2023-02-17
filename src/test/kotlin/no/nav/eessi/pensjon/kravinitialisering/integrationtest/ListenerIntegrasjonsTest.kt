package no.nav.eessi.pensjon.kravinitialisering.integrationtest

import com.ninjasquad.springmockk.MockkBean
import no.nav.eessi.pensjon.gcp.GcpStorageService
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.EessiPensjonKravInitialiseringTestApplication
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.kravinitialisering.config.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kravinitialisering.listener.Listener
import no.nav.eessi.pensjon.utils.toJson
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.ssl.TrustStrategy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpStatusCode
import org.mockserver.socket.PortFactory
import org.mockserver.verify.VerificationTimes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

private const val KRAV_INITIALISERING_TOPIC = "eessi-pensjon-krav-initialisering"
private lateinit var mockServer: ClientAndServer
private var mockServerPort = PortFactory.findFreePort()

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, ListenerIntegrasjonsTest.TestConfig::class, EessiPensjonKravInitialiseringTestApplication::class], value = ["SPRING_PROFILES_ACTIVE", "integrationtest"])
@ActiveProfiles("integrationtest")
@DirtiesContext
@EmbeddedKafka(
    controlledShutdown = true,
    partitions = 1,
    topics = [KRAV_INITIALISERING_TOPIC],
    brokerProperties = ["log.dir=out/embedded-kafkamottatt"]
)
class ListenerIntegrasjonsTest {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    lateinit var listener: Listener

    @MockkBean(relaxed = true)
    lateinit var gcpStorageService: GcpStorageService

    private lateinit var container: KafkaMessageListenerContainer<String, String>
    private lateinit var sedMottattProducerTemplate: KafkaTemplate<Int, String>

    @BeforeEach
    fun setup() {

        container = settOppUtitlityConsumer()
        container.start()
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)

        sedMottattProducerTemplate = settOppProducerTemplate()
    }

    @AfterEach
    fun after() {
        shutdown(container)
        embeddedKafka.kafkaServers.forEach { it.shutdown() }
    }

    @Test
    fun `En hendelse skal initalisere et krav`() {
        // -- alder

        val mockmodelAlder = BehandleHendelseModel(
            sakId = "12312312347",
            bucId = "1231231987",
            hendelsesKode = HendelseKode.SOKNAD_OM_ALDERSPENSJON,
            LocalDateTime.now(),
            "Test på beskrivelsen også"
        )

        sendMelding(mockmodelAlder)

        // -- uføre

        val mockmodel = BehandleHendelseModel(
            sakId = "123123123",
            bucId = "1231231",
            hendelsesKode = HendelseKode.SOKNAD_OM_UFORE,
            beskrivelse = "Test på beskrivelsen også"
        )
        val annenMockmodel = BehandleHendelseModel(
            sakId = "1231231234",
            bucId = "3231231",
            hendelsesKode = HendelseKode.SOKNAD_OM_UFORE,
            beskrivelse = "Test på beskrivelsen også"
        )

        val mockModelUtenOpprettdato =
            """
        {            
            "sakId" : "1231231111",
            "bucId" : "3231231",
            "hendelsesKode" : "SOKNAD_OM_UFORE",
            "beskrivelse" : "Test på beskrivelsen også"
        }            
        """.trimIndent()

        sendMelding(mockmodel)
        sendMelding(mockmodel)
        sendMelding(annenMockmodel)
        sendMeldingString(mockModelUtenOpprettdato)

        listener.getLatch().await(5000, TimeUnit.MILLISECONDS)

        verifyPostRequests(5)

    }

    private fun verifyPostRequests(antallPosts: Int) {

        mockServer.verify(
            HttpRequest.request()
                .withMethod(HttpMethod.POST.name())
                .withPath("/"),
            VerificationTimes.exactly(antallPosts)
        )
    }

    private fun sendMelding(melding: BehandleHendelseModel) {
        sedMottattProducerTemplate.sendDefault(melding.toJson())
    }

    private fun sendMeldingString(melding: String) {
        sedMottattProducerTemplate.sendDefault(melding)
    }

    private fun shutdown(container: KafkaMessageListenerContainer<String, String>) {
        container.stop()
        embeddedKafka.kafkaServers.forEach { it.shutdown() }
    }

    private fun settOppProducerTemplate(): KafkaTemplate<Int, String> {
        val senderProps = KafkaTestUtils.producerProps(embeddedKafka.brokersAsString)
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf)
        template.defaultTopic = KRAV_INITIALISERING_TOPIC
        return template
    }

    private fun settOppUtitlityConsumer(): KafkaMessageListenerContainer<String, String> {
        val consumerProperties = KafkaTestUtils.consumerProps(
            "eessi-pensjon-group2",
            "false",
            embeddedKafka
        )
        consumerProperties["auto.offset.reset"] = "earliest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProperties)
        val containerProperties = ContainerProperties(KRAV_INITIALISERING_TOPIC)
        val container = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        val messageListener = MessageListener<String, String> { record -> println("Konsumerer melding:  $record") }
        container.setupMessageListener(messageListener)

        return container
    }

    companion object {

        init {
            // Start Mockserver in memory

            mockServer = ClientAndServer.startClientAndServer(mockServerPort)
            System.setProperty("mockServerport", mockServerPort.toString())

            mockServer.`when`(
                HttpRequest.request()
                    .withMethod(HttpMethod.POST.name())
                    .withPath("/")
            )
                .respond(
                    HttpResponse.response()
                        .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("{}")
                )
        }

    }

    // Mocks PDL-PersonService and EuxService
    @Profile("integrationtest")
    @TestConfiguration
    class TestConfig {
        @Bean
        fun penAzureTokenRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
            val acceptingTrustStrategy = TrustStrategy { _: Array<X509Certificate?>?, _: String? -> true }

            val sslcontext: SSLContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build()
            val sslSocketFactory: SSLConnectionSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslcontext)
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build()
            val connectionManager: HttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build()
            val httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build()

            val customRequestFactory = HttpComponentsClientHttpRequestFactory()
            customRequestFactory.httpClient = httpClient

            return RestTemplateBuilder()
                .rootUri("https://localhost:${mockServerPort}")
                .build().apply {
                    requestFactory = customRequestFactory
                }
        }
    }
}
