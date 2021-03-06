package no.nav.eessi.pensjon.kravinitialisering.behandlehendelse

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModelPesys
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import javax.annotation.PostConstruct

@Component
class BehandleHendelseKlient(
    private val penBehandleHendelseOidcRestTemplate: RestTemplate,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(BehandleHendelseKlient::class.java)

    private lateinit var behandlehendelse: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        behandlehendelse = metricsHelper.init("Behandlehendelse", alert = MetricsHelper.Toggle.OFF)
    }

    fun kallOpprettBehandleHendelse(hendelseModel: BehandleHendelseModel) {
        try {
            opprettBehandleHendelse(BehandleHendelseModelPesys(
                sakId = hendelseModel.sakId,
                bucId = hendelseModel.bucId,
                hendelsesKode = hendelseModel.hendelsesKode,
                beskrivelse = hendelseModel.beskrivelse
            ))
            countKravinitSed(hendelseModel.hendelsesKode)
        } catch (ex: Exception) {
            logger.error(ex.message)
        }
    }

    fun countKravinitSed(hendelsesKode: HendelseKode) {
        try {
            Metrics.counter("Kravinit_Sed", "type", hendelsesKode.name).increment()
        } catch (e: Exception) {
            logger.warn("Metrics feilet p?? type: $hendelsesKode")
        }
    }

    private fun opprettBehandleHendelse(model: BehandleHendelseModelPesys) {
        behandlehendelse.measure {

            try {
                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_JSON
                val httpEntity = HttpEntity(model.toJson(), headers)
                logger.debug("*** legger f??lgende melding p?? behandlehendlse tjenesten: ${model.toJson()} ***")

                penBehandleHendelseOidcRestTemplate.postForEntity(
                    "/",
                    httpEntity,
                    String::class.java
                )
            } catch (ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under opprettelse av behandlehendlse ex: ", ex)
                throw RuntimeException("En feil oppstod under opprettelse av behandlehendelse ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch (ex: Exception) {
                logger.error("En feil oppstod under opprettelse av behandlehendlse ex: ", ex)
                throw RuntimeException("En feil oppstod under opprettelse av behandlehendelse ex: ${ex.message}")
            }

            logger.debug("*** Ferdig med melding ***")
        }
    }


}