package no.nav.eessi.pensjon.kravinitialisering.integrationtest

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.kravinitialisering.listener.Listener
import no.nav.eessi.pensjon.s3.S3StorageService
import no.nav.eessi.pensjon.security.sts.STSService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpStatusCode
import org.mockserver.verify.VerificationTimes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
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
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*

private const val KRAV_INITIALISERING_TOPIC = "eessi-pensjon-krav-initialisering"
private lateinit var mockServer: ClientAndServer

@SpringBootTest
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

    @MockBean(name = "pensjonsinformasjonOidcRestTemplate")
    lateinit var restEuxTemplate: RestTemplate

    @MockBean
    lateinit var stsService: STSService

    @Autowired
    private lateinit var s3StorageService: S3StorageService

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
            sakId = "123123123",
            bucId = "1231231987",
            hendelsesKode = HendelseKode.SOKNAD_OM_ALDERSPENSJON,
            "Test på beskrivelsen også"
        )

        sendMelding(mockmodelAlder)

        // -- uføre

        val mockmodel = BehandleHendelseModel(
            sakId = "123123123",
            bucId = "1231231",
            hendelsesKode = HendelseKode.SOKNAD_OM_UFORE,
            "Test på beskrivelsen også"
        )
        val annenMockmodel = BehandleHendelseModel(
            sakId = "123123123",
            bucId = "3231231",
            hendelsesKode = HendelseKode.SOKNAD_OM_UFORE,
            "Test på beskrivelsen også"
        )

        sendMelding(mockmodel)
        sendMelding(mockmodel)
        sendMelding(annenMockmodel)

        listener.getLatch().await(5000, TimeUnit.MILLISECONDS)

        verify()

    }

    private fun verify() {

        mockServer.verify(
            HttpRequest.request()
                .withMethod(HttpMethod.POST.name)
                .withPath("/"),
            VerificationTimes.exactly(3)
        )

    }

    private fun sendMelding(melding: BehandleHendelseModel) {
        sedMottattProducerTemplate.sendDefault(melding.toJson())
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
            val port = randomFrom()
            mockServer = ClientAndServer.startClientAndServer(port)
            System.setProperty("mockServerport", port.toString())
            // Mocker STS
            mockServer.`when`(
                HttpRequest.request()
                    .withMethod(HttpMethod.GET.name)
                    .withQueryStringParameter("grant_type", "client_credentials")
            )
                .respond(
                    HttpResponse.response()
                        .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(String(Files.readAllBytes(Paths.get("src/test/resources/STStoken.json"))))
                )

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


        private fun randomFrom(from: Int = 1024, to: Int = 65535): Int {
            val random = Random()
            return random.nextInt(to - from) + from
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
            storageService.env = "q1"
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
        fun s3StorageService(): S3StorageService {
            println("InintMock S3")
            return initMockS3()
        }
    }



}
