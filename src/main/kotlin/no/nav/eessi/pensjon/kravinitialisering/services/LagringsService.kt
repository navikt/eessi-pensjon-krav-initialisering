package no.nav.eessi.pensjon.kravinitialisering.services

import no.nav.eessi.pensjon.gcp.GcpStorageService
import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LagringsService (private val gcpStorageService: GcpStorageService) {

    private val logger = LoggerFactory.getLogger(LagringsService::class.java)

    fun lagreHendelseMedSakId(hendelse: BehandleHendelseModel) {
        val path = hentPathMedSakId(hendelse)

        try {
            val jsondata = hendelse.toJson()

            logger.debug("Lagrer hendelse: $path, data: $jsondata")
            gcpStorageService.lagre(path, jsondata)
        } catch (ex: Exception) {
            logger.error("Feiler ved lagring av data: $path")
        }
    }

    fun kanHendelsenOpprettes(hendelseModel: BehandleHendelseModel) : Boolean {
        logger.debug("liste over obj P_BUC_03/" + gcpStorageService.list("P_BUC_03/").toString())
        return !gcpStorageService.eksisterer(hentPathMedSakId(hendelseModel))
    }

    fun hentHendelse(hendelse: BehandleHendelseModel): BehandleHendelseModel? {
        val path = hentPathMedSakId(hendelse)
        logger.info("Henter sakId: ${hendelse.sakId} from $path")

        val objekt = try {
            gcpStorageService.hent(path)

        } catch (ex: Exception) {
            logger.error("Feiler ved henting av data : $path", ex)
            null
        }

        logger.debug("Henter hendelse fra: $path, data: $objekt")
        return try {
            objekt?.let { mapJsonToAny(it, typeRefs())}
        } catch (ex: Exception) {
            logger.error("Feilet ved mapping av json", ex)
            null
        }
    }


    fun hentPathMedSakId(hendelse: BehandleHendelseModel): String {
        val bucType = when (hendelse.hendelsesKode) {
            HendelseKode.SOKNAD_OM_UFORE -> "P_BUC_03"
            HendelseKode.SOKNAD_OM_ALDERSPENSJON -> "P_BUC_01"
            else -> {
                val msg = "Ikke gyldig hendelse for path. bucid: ${hendelse.sakId}"
                throw RuntimeException(msg).also { logger.error(msg) }
            }
        }
        val path =  "$bucType/sakid=${hendelse.sakId}.json"
        logger.info("Hendelsespath: $path")

        return path
    }
}