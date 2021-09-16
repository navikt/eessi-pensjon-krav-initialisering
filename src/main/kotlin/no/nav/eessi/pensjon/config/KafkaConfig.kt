package no.nav.eessi.pensjon.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.time.Duration

@EnableKafka
@Profile("test", "prod")
@Configuration
class KafkaConfig(
    @param:Value("\${kafka.keystore.path}") private val keystorePath: String,
    @param:Value("\${kafka.credstore.password}") private val credstorePassword: String,
    @param:Value("\${kafka.truststore.path}") private val truststorePath: String,
    @param:Value("\${kafka.brokers}") private val aivenBootstrapServers: String,
    @param:Value("\${ONPREM_KAFKA_BOOTSTRAP_SERVERS_URL}") private val onpremBootstrapServers: String,
    @param:Value("\${kafka.security.protocol}") private val securityProtocol: String,
    @param:Value("\${srvusername}") private val srvusername: String,
    @param:Value("\${srvpassword}") private val srvpassword: String,
    @Autowired val kafkaErrorHandler: KafkaCustomErrorHandler
) {

    fun aivenKafkaConsumerFactory(): ConsumerFactory<String, String> {
        val keyDeserializer: JsonDeserializer<String> = JsonDeserializer(String::class.java)
        keyDeserializer.setRemoveTypeHeaders(true)
        keyDeserializer.addTrustedPackages("*")
        keyDeserializer.setUseTypeHeaders(false)
        val valueDeserializer = StringDeserializer()
        val configMap: MutableMap<String, Any> = HashMap()
        populerAivenCommonConfig(configMap)
        configMap[ConsumerConfig.CLIENT_ID_CONFIG] = "eessi-pensjon-krav-initialisering"
        configMap[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = aivenBootstrapServers
        configMap[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        return DefaultKafkaConsumerFactory(configMap, keyDeserializer, valueDeserializer)
    }

    fun onpremKafkaConsumerFactory(): ConsumerFactory<String, String> {
        val keyDeserializer: JsonDeserializer<String> = JsonDeserializer(String::class.java)
        keyDeserializer.setUseTypeHeaders(false)
        val configMap: MutableMap<String, Any> = HashMap()
        populerOnpremCommonConfig(configMap)
        configMap[ConsumerConfig.CLIENT_ID_CONFIG] = "eessi-pensjon-krav-initialisering"
        configMap[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = onpremBootstrapServers
        configMap[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        configMap[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        configMap[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        return DefaultKafkaConsumerFactory(configMap, StringDeserializer(), JsonDeserializer())
    }

    @Bean
    fun aivenKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = aivenKafkaConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.containerProperties.authorizationExceptionRetryInterval =  Duration.ofSeconds(4L)
        return factory
    }

    @Bean
    fun onpremKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = onpremKafkaConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.containerProperties.authorizationExceptionRetryInterval =  Duration.ofSeconds(4L)
        factory.setErrorHandler(kafkaErrorHandler)
        factory.setRetryTemplate(retryTemplate())
        return factory
    }

    private fun populerAivenCommonConfig(configMap: MutableMap<String, Any>) {
        configMap[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
        configMap[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword
        configMap[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword
        configMap[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword
        configMap[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        configMap[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        configMap[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
        configMap[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = securityProtocol
    }

    private fun populerOnpremCommonConfig(configMap: MutableMap<String, Any>) {
        configMap[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        configMap[SaslConfigs.SASL_MECHANISM] = "PLAIN"
        configMap[SaslConfigs.SASL_JAAS_CONFIG] = "org.apache.kafka.common.security.plain.PlainLoginModule required username=${srvusername} password=${srvpassword};"
    }

    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()

        val fixedBackOffPolicy = FixedBackOffPolicy()
        fixedBackOffPolicy.backOffPeriod = 1000
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy)

        val retryPolicy = SimpleRetryPolicy()
        retryPolicy.maxAttempts = 1
        retryTemplate.setRetryPolicy(retryPolicy)

        return retryTemplate
    }

}

//import org.apache.kafka.clients.consumer.Consumer
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.slf4j.LoggerFactory
//import org.springframework.boot.ApplicationRunner
//import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.context.annotation.Profile
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry
//import org.springframework.kafka.core.ConsumerFactory
//import org.springframework.kafka.listener.ContainerAwareErrorHandler
//import org.springframework.kafka.listener.ContainerStoppingErrorHandler
//import org.springframework.kafka.listener.MessageListenerContainer
//import org.springframework.kafka.listener.SeekToCurrentErrorHandler
//import org.springframework.retry.backoff.FixedBackOffPolicy
//import org.springframework.retry.policy.SimpleRetryPolicy
//import org.springframework.retry.support.RetryTemplate
//import org.springframework.util.backoff.FixedBackOff
//import java.io.PrintWriter
//import java.io.StringWriter
//import java.time.Duration
//
//@Configuration
//class KafkaConfig {
//
//    @Bean
//    fun sedSendtAuthRetry(registry: KafkaListenerEndpointRegistry): ApplicationRunner? {
//        return ApplicationRunner {
//            val kravInitialiseringListener = registry.getListenerContainer("kravInitialiseringListener")
//            kravInitialiseringListener.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(4L)
//            kravInitialiseringListener.start()
//          }
//    }
//
//    @Bean
//    fun kafkaListenerContainerFactory(configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
//                                      kafkaConsumerFactory: ConsumerFactory<Any, Any>): ConcurrentKafkaListenerContainerFactory<*, *>  {
//        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
//        factory.setRetryTemplate(retryTemplate())
//        configurer.configure(factory, kafkaConsumerFactory)
//        //factory.setErrorHandler(KafkaCustomErrorHandlerBean())
//        factory.setErrorHandler(SeekToCurrentErrorHandler(FixedBackOff(1,0)))
//        return factory
//    }
//

//
//    @Profile("prod")
//    @Bean
//    fun kafkaCustomErrorHandlerBean() : KafkaCustomErrorHandler{
//        return KafkaCustomErrorHandler()
//    }