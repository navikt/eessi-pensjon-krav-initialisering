package no.nav.eessi.pensjon.kravinitialisering.behandlehendelse

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
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
class BehandleHendelseKlient(private val penBehandleHendelseOidcRestTemplate: RestTemplate,
     @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

     private val logger = LoggerFactory.getLogger(BehandleHendelseKlient::class.java)

     private lateinit var behandlehendelse: MetricsHelper.Metric

     @PostConstruct
     fun initMetrics() {
         behandlehendelse = metricsHelper.init("Behandlehendelse")
         opprettBehandleHendelse(
             BehandleHendelseModel(
             "22929983",
                 "",
             HendelseKode.SOKNAD_OM_UFORE,
            "22929983 - UFØRETRYGD PBUC03 CP 1286518"
             )
         )
     }

    fun opprettBehandleHendelse(model: BehandleHendelseModel) {
        behandlehendelse.measure {

            try {

                val urlpath = "/"
                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_JSON
                val httpEntity = HttpEntity(model.toJson(), headers)
                logger.debug("*** legger følgende melding på behandlehendlse tjenesten: ${model.toJson()} ***")

                penBehandleHendelseOidcRestTemplate.postForEntity(
                    "/",
                    httpEntity,
                    String::class.java
                )
            } catch (ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under opprettelse av behandlehendlse ex: ", ex)
                throw RuntimeException("En feil oppstod under opprettelse av behandlehendlse ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch (ex: Exception) {
                logger.error("En feil oppstod under opprettelse av behandlehendlse ex: ", ex)
                throw RuntimeException("En feil oppstod under opprettelse av behandlehendlse ex: ${ex.message}")
            }

            logger.debug("*** Ferdig med melding ***")
        }
    }


}