package no.nav.eessi.pensjon.kravinitialisering

import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import no.nav.eessi.pensjon.s3.S3StorageService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(private val hendelseKlient: BehandleHendelseKlient, private val s3StorageService: S3StorageService) {

    @PostMapping("kravinit", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(@RequestBody model: BehandleHendelseModel) {

        if (model.hendelsesKode == HendelseKode.SOKNAD_OM_ALDERSPENSJON) {
            hendelseKlient.kallOpprettBehandleHendelse(model)
        }

    }

    @GetMapping("bucs")
    fun listAllPBuc(): String {

        val tmp = s3StorageService.list("P_BUC_01") + s3StorageService.list("P_BUC_03")
        val listPbuc01And03 = tmp.sorted()

        val sb = StringBuilder()
        listPbuc01And03.map {
            val jsonObj = s3StorageService.get(it)
            Pair(it, mapJsonToAny(jsonObj!!, typeRefs<BehandleHendelseModel>()))
            }.sortedBy { it.second.opprettetDato }.forEach {

            sb.append("Lister elementer: ").append(it.first).append("\n")
            sb.append("-------hendelse---------\n")
            sb.append(it.toJson()).append("\n")
            sb.append("----------------------------------------\n")
        }

        return sb.toString()
    }


}