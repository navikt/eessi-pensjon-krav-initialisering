package no.nav.eessi.pensjon.kravinitialisering.listener

import no.nav.eessi.pensjon.eux.model.sed.X005
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import no.nav.eessi.pensjon.kravinitialisering.services.LagringsService
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CountDownLatch

private const val X_REQUEST_ID = "x_request_id"

@Component
class Listener(
    private val hendelseKlient: BehandleHendelseKlient,
    private val lagringsService: LagringsService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {

    private val logger = LoggerFactory.getLogger(Listener::class.java)
    private val latch = CountDownLatch(4)

    private var opprettKrav: MetricsHelper.Metric
    private var opprettKravFinnes: MetricsHelper.Metric

    fun getLatch() = latch

    init {
        opprettKrav = metricsHelper.init("opprettKrav", alert = MetricsHelper.Toggle.OFF)
        opprettKravFinnes = metricsHelper.init("opprettKravFinnes", alert = MetricsHelper.Toggle.OFF)
    }

    @KafkaListener(
        containerFactory = "aivenKafkaListenerContainerFactory",
        topics = ["\${kafka.krav.initialisering.topic}"],
        groupId = "\${kafka.krav.initialisering.groupid}",
    )
    fun consumeOpprettelseMelding(
        hendelse: String,
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        MDC.putCloseable(X_REQUEST_ID, createUUID(cr)).use {
            logger.info("Melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
            try {
                logger.debug("Hendelse : ${hendelse.toJson()}")
                val hendelseModel: BehandleHendelseModel = mapJsonToAny(hendelse)

                if (lagringsService.kanHendelsenOpprettes(hendelseModel)) {
                    opprettKrav.measure {
                        logger.info("Hendelse offset: ${cr.offset()}, finnes ikke fra før. Oppretter krav ${hendelseModel.hendelsesKode}, bucid: ${hendelseModel.bucId} saknr: ${hendelseModel.sakId}")

                        hendelseKlient.kallOpprettBehandleHendelse(hendelseModel)
                        lagringsService.lagreHendelseMedSakId(hendelseModel)
                    }
                } else {
                    opprettKravFinnes.measure {
                        logger.info("Hendelse offset: ${cr.offset()}, finnes fra før. Krav ${hendelseModel.hendelsesKode}, bucid: ${hendelseModel.bucId} saknr: ${hendelseModel.sakId}")
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

    private fun createUUID(cr: ConsumerRecord<String, String>): String {
        val key = cr.key() ?: UUID.randomUUID().toString()
        logger.debug("$X_REQUEST_ID : $key")
        return key
    }
}

