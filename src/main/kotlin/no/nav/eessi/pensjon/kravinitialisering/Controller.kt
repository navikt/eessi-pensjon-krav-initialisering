package no.nav.eessi.pensjon.kravinitialisering

import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import no.nav.eessi.pensjon.kravinitialisering.services.LagringsService
import no.nav.eessi.pensjon.s3.S3StorageService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

//Test midlertidig
@RestController
class Controller(private val hendelseKlient: BehandleHendelseKlient, private val s3StorageService: S3StorageService) {

    @PostMapping("kravinit", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(@RequestBody model: BehandleHendelseModel) {

        if (model.hendelsesKode == HendelseKode.SOKNAD_OM_ALDERSPENSJON) {
            hendelseKlient.kallOpprettBehandleHendelse(model)
        }

    }

    @GetMapping("bucs")
    fun listAllPBuc03() {

        val list = s3StorageService.list("P_BUC_03")

        //"P_BUC_03/123123123" -> json
        val lagre = LagringsService(s3StorageService)

        list.forEach {
            println("List element: $it")

            val jsonObj = s3StorageService.get(it)
            val hendelse = mapJsonToAny(jsonObj!!, typeRefs<BehandleHendelseModel>())


            val path = lagre.hentPath(hendelse)
            println("path: $path")
            //lagre.lagreHendelse(hendelse)
        }

    }

}