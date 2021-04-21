package no.nav.eessi.pensjon.kravinitialisering.listener

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import no.nav.eessi.pensjon.kravinitialisering.services.LagringsService
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.*
import javax.annotation.PostConstruct

@Component
class Listener(
    private val hendelseKlient: BehandleHendelseKlient,
    private val lagringsService: LagringsService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(Listener::class.java)

    private val latch = CountDownLatch(4)

    private lateinit var opprettKrav: MetricsHelper.Metric
    private lateinit var opprettKravFinnes: MetricsHelper.Metric

    fun getLatch() = latch

    @PostConstruct
    fun initMetrics() {
        opprettKrav = metricsHelper.init("opprettKrav", alert = MetricsHelper.Toggle.OFF)
        opprettKravFinnes = metricsHelper.init("opprettKravFinnes", alert = MetricsHelper.Toggle.OFF)
    }

    @KafkaListener(
        id = "kravInitialiseringListener",
        idIsGroup = false,
        topics = ["\${kafka.krav.initialisering.topic}"],
        groupId = "\${kafka.krav.initialisering.groupid}",
        autoStartup = "false"
    )
    fun consumeOpprettelseMelding(
        hendelse: String,
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            logger.info("Innkommet hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")

            try {
                logger.debug("Hendelse : ${hendelse.toJson()}")
                val hendelseModel: BehandleHendelseModel = mapJsonToAny(hendelse, typeRefs())

                if (hendelseModel.hendelsesKode == HendelseKode.SOKNAD_OM_ALDERSPENSJON) {
                    logger.debug("Hendelse krav ${hendelseModel.hendelsesKode}, bucid: ${hendelseModel.bucId} saknr: ${hendelseModel.sakId}")

                    hendelseKlient.kallOpprettBehandleHendelse(hendelseModel)
                }

                if (hendelseModel.hendelsesKode == HendelseKode.SOKNAD_OM_UFORE) {
                    if (lagringsService.kanHendelsenOpprettes(hendelseModel)) {
                        opprettKrav.measure {
                            logger.debug("Hendelse finnes ikke fra før. Oppretter krav bucid: ${hendelseModel.bucId} saknr: ${hendelseModel.sakId}")

                            hendelseKlient.kallOpprettBehandleHendelse(hendelseModel)
                            lagringsService.lagreHendelse(hendelseModel)
                        }
                    } else {
                        opprettKravFinnes.measure {
                            logger.debug("Hendelse finnes og krav er opprettet bucid: ${hendelseModel.bucId} saknr: ${hendelseModel.sakId}")
                        }
                    }
                }

                acknowledgment.acknowledge()
                logger.info("Acket melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
            } catch (ex: Exception) {
                logger.error("Noe gikk galt under behandling av hendelse:\n $hendelse \n", ex)
                throw RuntimeException(ex.message)
            }
            latch.countDown()
        }
    }
}

