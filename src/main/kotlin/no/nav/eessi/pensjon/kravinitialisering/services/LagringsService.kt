package no.nav.eessi.pensjon.kravinitialisering.services

import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.s3.S3StorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LagringsService (private val s3StorageService: S3StorageService) {

    private val logger = LoggerFactory.getLogger(LagringsService::class.java)

    fun lagreHendelse(hendelse: BehandleHendelseModel) {
        val path = hentPath(hendelse)

        try {
            val jsondata = hendelse.toJson()

            logger.debug("Lagrer hendelse: $path, data: $jsondata")
            s3StorageService.put(path, jsondata)
        } catch (ex: Exception) {
            logger.error("Feiler ved lagring av data: $path")
        }
    }

    fun lagreHendelseMedSakId(hendelse: BehandleHendelseModel) {
        val path = hentPathMedSakId(hendelse)

        try {
            val jsondata = hendelse.toJson()

            logger.debug("Lagrer hendelse: $path, data: $jsondata")
            s3StorageService.put(path, jsondata)
        } catch (ex: Exception) {
            logger.error("Feiler ved lagring av data: $path")
        }
    }

    fun kanHendelsenOpprettes(hendelseModel: BehandleHendelseModel) = hentHendelse(hendelseModel) == null

    fun hentHendelse(hendelse: BehandleHendelseModel): BehandleHendelseModel? {
        val path = hentPath(hendelse)
        logger.info("Henter bucId: ${hendelse.bucId} from $path")

        return try {
            val hendelseModel = s3StorageService.get(path)

            logger.debug("Henter hendelse fra: $path, data: $hendelseModel")
            hendelseModel?.let { mapJsonToAny(it, typeRefs()) }

        } catch (ex: Exception) {
            logger.info("Feiler ved henting av data : $path")
            null
        }
    }

    fun hentPath(hendelse: BehandleHendelseModel): String {
        val bucType = when (hendelse.hendelsesKode) {
            HendelseKode.SOKNAD_OM_UFORE -> "P_BUC_03"
            HendelseKode.SOKNAD_OM_ALDERSPENSJON -> "P_BUC_01"
            else -> {
                val msg = "Ikke gyldig hendelse for path. bucid: ${hendelse.bucId}"
                throw RuntimeException(msg).also { logger.error(msg) }
            }
        }
        val path =  "$bucType/${hendelse.bucId}"
        logger.info("Hendelsespath: $path")

        return path
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