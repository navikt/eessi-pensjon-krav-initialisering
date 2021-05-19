package no.nav.eessi.pensjon.kravinitialisering

import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
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
    fun listAllPBuc(): String {


        val tmp = s3StorageService.list("P_BUC_01") + s3StorageService.list("P_BUC_03")
        val listPbuc01And03 = tmp.sorted()

        val sb = StringBuilder()
        listPbuc01And03.forEach {
            sb.append("Lister elementer: ").append(it).append("\n")

            val jsonObj = s3StorageService.get(it)
            val hendelse = mapJsonToAny(jsonObj!!, typeRefs<BehandleHendelseModel>())

            sb.append("-------hendelse---------\n")
            sb.append(hendelse.toJson()).append("\n")
            sb.append("----------------------------------------\n")
        }

        return sb.toString()
    }


    @PostMapping("konvertering")
    fun konverterPathTilNyttFormat() {
        konverterAlleGamleBucFraBucIDTilSakId()
    }

    fun konverterAlleGamleBucFraBucIDTilSakId() {
        val listPbuc01And03 = s3StorageService.list("P_BUC_01") + s3StorageService.list("P_BUC_03")

        val lagringsService = LagringsService(s3StorageService)

        //konverer kun buc uten sak (altså av gammel type)
        listPbuc01And03.forEach {
            val jsonObj = s3StorageService.get(it)
            val hendelse = mapJsonToAny(jsonObj!!, typeRefs<BehandleHendelseModel>())
            hendelse.let {
                lagringsService.lagreHendelseMedSakId(hendelse)
            }
        }
    }
}