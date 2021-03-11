//package no.nav.eessi.pensjon.behandleutland.listener.architecture.integrationtest
//
//import com.nhaarman.mockitokotlin2.any
//import com.nhaarman.mockitokotlin2.whenever
//import io.mockk.mockk
//import io.mockk.spyk
//import io.mockk.verify
//import no.nav.eessi.pensjon.eux.BucMetadata
//import no.nav.eessi.pensjon.eux.BucType
//import no.nav.eessi.pensjon.eux.EuxService
//import no.nav.eessi.pensjon.s3.S3StorageService
//import no.nav.eessi.pensjon.security.sts.STSService
//import no.nav.eessi.pensjon.statistikk.listener.BehandleUtlandListener
//import no.nav.eessi.pensjon.statistikk.models.OpprettelseType
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockserver.integration.ClientAndServer
//import org.mockserver.model.Header
//import org.mockserver.model.HttpRequest
//import org.mockserver.model.HttpResponse
//import org.mockserver.model.HttpStatusCode
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.context.TestConfiguration
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.context.annotation.Bean
//import org.springframework.http.HttpMethod
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory
//import org.springframework.kafka.core.DefaultKafkaProducerFactory
//import org.springframework.kafka.core.KafkaTemplate
//import org.springframework.kafka.listener.ContainerProperties
//import org.springframework.kafka.listener.KafkaMessageListenerContainer
//import org.springframework.kafka.listener.MessageListener
//import org.springframework.kafka.test.EmbeddedKafkaBroker
//import org.springframework.kafka.test.context.EmbeddedKafka
//import org.springframework.kafka.test.utils.ContainerTestUtils
//import org.springframework.kafka.test.utils.KafkaTestUtils
//import org.springframework.test.annotation.DirtiesContext
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.web.client.RestTemplate
//import java.nio.file.Files
//import java.nio.file.Paths
//import java.util.*
//import java.util.concurrent.TimeUnit
//
//private const val STATISTIKK_TOPIC = "eessi-pensjon-statistikk-inn"
//private lateinit var mockServer: ClientAndServer
//
//@SpringBootTest
//@ActiveProfiles("integrationtest")
//@DirtiesContext
//@EmbeddedKafka(
//    controlledShutdown = true,
//    partitions = 1,
//    topics = [STATISTIKK_TOPIC],
//    brokerProperties = ["log.dir=out/embedded-kafkamottatt"]
//)
//class StatistikkListenerIntegrasjonsTest {
//
//    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//    @Autowired
//    lateinit var embeddedKafka: EmbeddedKafkaBroker
//
//    @MockBean(name = "pensjonsinformasjonOidcRestTemplate")
//    lateinit var restEuxTemplate: RestTemplate
//
//    @MockBean
//    lateinit var euxService: EuxService
//
//    @MockBean
//    lateinit var s3StorageService: S3StorageService
//
//    @MockBean
//    lateinit var stsService: STSService
//
//    @Autowired
//    lateinit var behandleUtlandListener: BehandleUtlandListener
//
//    @Autowired
//    lateinit var statistikkPublisher: StatistikkPublisher
//
//    lateinit var container: KafkaMessageListenerContainer<String, String>
//    lateinit var sedMottattProducerTemplate: KafkaTemplate<Int, String>
//
//    @BeforeEach
//    fun setup() {
//        container = settOppUtitlityConsumer(STATISTIKK_TOPIC)
//        container.start()
//        ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
//
//        sedMottattProducerTemplate = settOppProducerTemplate()
//    }
//
//    @AfterEach
//    fun after() {
//        shutdown(container)
//        embeddedKafka.kafkaServers.forEach { it.shutdown() }
//    }
//
//    @Test
//    fun `En buc-hendelse skal sendes videre til riktig kanal  `() {
//        val bucMetadata  = BucMetadata (listOf(), BucType.P_BUC_01, "2020-12-08T09:52:55.345+0000")
//
//        whenever(euxService.getBucMetadata(any())).thenReturn(bucMetadata)
//
//        val budMelding = OpprettelseMelding(
//            opprettelseType = OpprettelseType.BUC,
//            rinaId = "123",
//            dokumentId = "d740047e730f475aa34ae59f62e3bb99",
//            vedtaksId = null
//        )
//
//        sendMelding(budMelding).let {
//            behandleUtlandListener.getLatch().await(15000, TimeUnit.MILLISECONDS)
//        }
//        verify(exactly = 1) { statistikkPublisher.publiserBucOpprettetStatistikk(any()) }
//    }
//
//    private fun sendMelding(melding: OpprettelseMelding) {
//        sedMottattProducerTemplate.sendDefault(melding.toJson())
//    }
//
//    private fun shutdown(container: KafkaMessageListenerContainer<String, String>) {
//        container.stop()
//        embeddedKafka.kafkaServers.forEach { it.shutdown() }
//    }
//
//    private fun settOppProducerTemplate(): KafkaTemplate<Int, String> {
//        val senderProps = KafkaTestUtils.producerProps(embeddedKafka.brokersAsString)
//        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
//        val template = KafkaTemplate(pf)
//        template.defaultTopic = STATISTIKK_TOPIC
//        return template
//    }
//
//    private fun settOppUtitlityConsumer(topicNavn: String): KafkaMessageListenerContainer<String, String> {
//        val consumerProperties = KafkaTestUtils.consumerProps(
//            "eessi-pensjon-group2",
//            "false",
//            embeddedKafka
//        )
//        consumerProperties["auto.offset.reset"] = "earliest"
//
//        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProperties)
//        val containerProperties = ContainerProperties(topicNavn)
//        val container = KafkaMessageListenerContainer<String, String>(consumerFactory, containerProperties)
//        val messageListener = MessageListener<String, String> { record -> println("Konsumerer melding:  $record") }
//        container.setupMessageListener(messageListener)
//
//        return container
//    }
//
//    companion object {
//
//        init {
//            // Start Mockserver in memory
//            val port = randomFrom()
//            mockServer = ClientAndServer.startClientAndServer(port)
//            System.setProperty("mockServerport", port.toString())
//            // Mocker STS
//            mockServer.`when`(
//                HttpRequest.request()
//                    .withMethod(HttpMethod.GET.name)
//                    .withQueryStringParameter("grant_type", "client_credentials")
//            )
//                .respond(
//                    HttpResponse.response()
//                        .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
//                        .withStatusCode(HttpStatusCode.OK_200.code())
//                        .withBody(String(Files.readAllBytes(Paths.get("src/test/resources/STStoken.json"))))
//                )
//
//            mockServer.`when`(
//                HttpRequest.request()
//                    .withMethod(HttpMethod.GET.name)
//                    .withPath("/buc/123")
//            )
//                .respond(
//                    HttpResponse.response()
//                        .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
//                        .withStatusCode(HttpStatusCode.OK_200.code())
//                        .withBody(String(Files.readAllBytes(Paths.get("src/test/resources/buc/bucMedP2000.json"))))
//                )
//        }
//
//
//        private fun randomFrom(from: Int = 1024, to: Int = 65535): Int {
//            val random = Random()
//            return random.nextInt(to - from) + from
//        }
//    }
//
//    @TestConfiguration
//    class TestConfig {
//
//        @Bean
//        fun statistikkPublisher(): StatistikkPublisher {
//            return spyk(StatistikkPublisher(mockk(relaxed = true), "bogusTopic"))
//        }
//    }
//
//}
