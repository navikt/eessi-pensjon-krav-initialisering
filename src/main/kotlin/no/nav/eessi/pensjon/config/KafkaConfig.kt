package no.nav.eessi.pensjon.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerAwareErrorHandler
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.backoff.FixedBackOff
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration

@Configuration
class KafkaConfig {

    @Bean
    fun sedSendtAuthRetry(registry: KafkaListenerEndpointRegistry): ApplicationRunner? {
        return ApplicationRunner {
            val kravInitialiseringListener = registry.getListenerContainer("kravInitialiseringListener")
            kravInitialiseringListener.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(4L)
            kravInitialiseringListener.start()
          }
    }

    @Bean
    fun kafkaListenerContainerFactory(configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
                                      kafkaConsumerFactory: ConsumerFactory<Any, Any>): ConcurrentKafkaListenerContainerFactory<*, *>  {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.setRetryTemplate(retryTemplate())
        configurer.configure(factory, kafkaConsumerFactory)
        //factory.setErrorHandler(KafkaCustomErrorHandlerBean())
        factory.setErrorHandler(SeekToCurrentErrorHandler(FixedBackOff(1,-1)))
        return factory
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

    @Profile("prod")
    @Bean
    fun kafkaCustomErrorHandlerBean() : KafkaCustomErrorHandler{
        return KafkaCustomErrorHandler()
    }

    open class KafkaCustomErrorHandler : ContainerAwareErrorHandler {
        private val logger = LoggerFactory.getLogger(KafkaCustomErrorHandler::class.java)

        private val stopper = ContainerStoppingErrorHandler()

        override fun handle(thrownException: Exception?,
                            records: MutableList<ConsumerRecord<*, *>>?,
                            consumer: Consumer<*, *>?,
                            container: MessageListenerContainer?) {
            val stacktrace = StringWriter()
            thrownException?.printStackTrace(PrintWriter(stacktrace))

            logger.error("En feil oppstod under kafka konsumering av meldinger: \n ${hentMeldinger(records)} \n" +
                    "Stopper containeren ! Restart er nødvendig for å fortsette konsumering, $stacktrace")
            stopper.handle(thrownException, records, consumer, container)
        }

        fun hentMeldinger(records: MutableList<ConsumerRecord<*, *>>?): String {
            var meldinger = ""
            records?.forEach { it ->
                meldinger += "--------------------------------------------------------------------------------\n"
                meldinger += it.toString()
                meldinger += "\n"
            }
            return meldinger
        }
    }

}