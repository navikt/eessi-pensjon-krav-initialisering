package no.nav.eessi.pensjon.behandleutland.listener.architecture.integrationtest

import no.nav.eessi.pensjon.kravinitialisering.listener.Listener
import no.nav.eessi.pensjon.security.sts.STSService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
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
import java.util.*
import java.util.concurrent.TimeUnit

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
    lateinit var listener: Listener

    lateinit var container: KafkaMessageListenerContainer<String, String>
    lateinit var sedMottattProducerTemplate: KafkaTemplate<Int, String>

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
    fun `En buc-hendelse skal sendes videre til riktig kanal  `() {

        sendMelding("Test").let {
            listener.getLatch().await(15000, TimeUnit.MILLISECONDS)
        }
        //verify(exactly = 1) { statistikkPublisher.publiserBucOpprettetStatistikk(any()) }
    }

    private fun sendMelding(melding: String) {
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


        private fun randomFrom(from: Int = 1024, to: Int = 65535): Int {
            val random = Random()
            return random.nextInt(to - from) + from
        }
    }

    @TestConfiguration
    class TestConfig {

//        @Bean
//        fun statistikkPublisher(): StatistikkPublisher {
//            return spyk(StatistikkPublisher(mockk(relaxed = true), "bogusTopic"))
//        }
    }

}
