package no.nav.eessi.pensjon.config

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.ConsumerFactory
import java.time.Duration

@Configuration
class KafkaConfig {

    @Bean
    fun sedSendtAuthRetry(registry: KafkaListenerEndpointRegistry): ApplicationRunner? {
        return ApplicationRunner {
            val behandleUtlandListener = registry.getListenerContainer("behandleUtlandListener")
            behandleUtlandListener.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(4L)
            behandleUtlandListener.start()

              }
    }

    @Bean
    fun kafkaListenerContainerFactory(configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
                                      kafkaConsumerFactory: ConsumerFactory<Any, Any>): ConcurrentKafkaListenerContainerFactory<*, *>  {

        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        configurer.configure(factory, kafkaConsumerFactory)
     //   factory.setErrorHandler(kafkaErrorHandler)
        return factory
    }

/*    @Bean
    fun KafkaCustomErrorHandlerBean() : KafkaCustomErrorHandler{
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
    }*/
}