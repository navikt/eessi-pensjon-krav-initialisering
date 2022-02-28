package no.nav.eessi.pensjon.kravinitialisering.integrationtest

import IntegrasjonsTestConfig
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.EessiPensjonKravInitialiseringTestApplication
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.kravinitialisering.listener.Listener
import no.nav.eessi.pensjon.s3.S3StorageService
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.apache.http.ssl.TrustStrategy
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
import java.net.ServerSocket
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.util.concurrent.*
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

    private lateinit var container: KafkaMessageListenerContainer<String, String>
    private lateinit var sedMottattProducerTemplate: KafkaTemplate<Int, String>

    @BeforeEach
    fun setup() {

        container = settOppUtitlityConsumer(KRAV_INITIALISERING_TOPIC)
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
                .withMethod(HttpMethod.POST.name)
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

    private fun settOppUtitlityConsumer(topicNavn: String): KafkaMessageListenerContainer<String, String> {
        val consumerProperties = KafkaTestUtils.consumerProps(
            "eessi-pensjon-group2",
            "false",
            embeddedKafka
        )
        consumerProperties["auto.offset.reset"] = "earliest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProperties)
        val containerProperties = ContainerProperties(topicNavn)
        val container = KafkaMessageListenerContainer<String, String>(consumerFactory, containerProperties)
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
                    .withMethod(HttpMethod.POST.name)
                    .withPath("/")
            )
                .respond(
                    HttpResponse.response()
                        .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("{}")
                )
        }

        fun initMockS3(): S3StorageService {
            val s3Port = ServerSocket(0).use { it.localPort }

            val s3api = S3Mock.Builder().withPort(s3Port).withInMemoryBackend().build()
            s3api.start()
            val endpoint = AwsClientBuilder.EndpointConfiguration("http://localhost:$s3Port", "us-east-1")

            val s3MockClient = AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(true)
                .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
                .withEndpointConfiguration(endpoint)
                .build()

            s3MockClient.createBucket("eessipensjon")
            //return s3MockClient
            val storageService = S3StorageService(s3MockClient)
            storageService.bucketname = "eessipensjon"
            storageService.init()
            return storageService
        }
    }

    // Mocks PDL-PersonService and EuxService
    @Suppress("unused")
    @Profile("integrationtest")
    @TestConfiguration
    class TestConfig {

        @Bean
        fun s3StorageService (): S3StorageService{
           return initMockS3()
        }

        @Bean
        fun penAzureTokenRestTemplate(templateBuilder: RestTemplateBuilder): RestTemplate {
            val acceptingTrustStrategy = TrustStrategy { chain: Array<X509Certificate?>?, authType: String? -> true }

            val sslContext: SSLContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build()

            val httpClient: CloseableHttpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
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
