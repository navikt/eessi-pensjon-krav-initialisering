
import no.nav.eessi.pensjon.config.KafkaCustomErrorHandler
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.test.EmbeddedKafkaBroker
import java.time.Duration

@TestConfiguration
class IntegrasjonsTestConfig(@Autowired val kafkaErrorHandler: KafkaCustomErrorHandler) {
    @Value("\${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
    private lateinit var brokerAddresses: String

    fun aivenKafkaConsumerFactory(): ConsumerFactory<String, String> {
        val configMap: MutableMap<String, Any> = HashMap()
        populerAivenCommonConfig(configMap)
        configMap[ConsumerConfig.CLIENT_ID_CONFIG] = "eessi-pensjon-krav-initialisering"
        configMap[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerAddresses
        configMap[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        configMap[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        configMap[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        return DefaultKafkaConsumerFactory(configMap, StringDeserializer(), StringDeserializer())
    }

    @Bean
    fun aivenKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = aivenKafkaConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(4L))
        return factory
    }

    private fun populerAivenCommonConfig(configMap: MutableMap<String, Any>) {
        configMap[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "PLAINTEXT"
    }
}