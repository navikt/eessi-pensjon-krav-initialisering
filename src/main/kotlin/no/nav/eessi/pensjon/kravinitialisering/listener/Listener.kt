package no.nav.eessi.pensjon.kravinitialisering.listener

import org.slf4j.LoggerFactory
import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import no.nav.eessi.pensjon.kravinitialisering.services.LagringsService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CountDownLatch

@Component
class Listener(private val behandleHendelse: BehandleHendelseKlient, private val lagringsService: LagringsService) {

    private val logger = LoggerFactory.getLogger(Listener::class.java)

    private val latch = CountDownLatch(1)

    fun getLatch() = latch

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
                val model: BehandleHendelseModel = mapJsonToAny(hendelse, typeRefs())

                if (lagringsService.hentHendelse(model) == null) {
                    //støtter kun P2200 for øyeblikket!
                    logger.debug("Hendelse finnes ikke fra før. Oppretter krav bucid: ${model.bucId} saknr: ${model.sakId}")
                    if (model.hendelsesKode == HendelseKode.SOKNAD_OM_UFORE) {
                        try{
                            behandleHendelse.opprettBehandleHendelse(model)
                        }
                        catch (ex:Exception){
                            logger.error(ex.message)
                        }
                        lagringsService.lagreHendelse(model)
                    }
                } else {
                    logger.debug("Hendelse finnes og krav er opprettet bucid: ${model.bucId} saknr: ${model.sakId}")
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

