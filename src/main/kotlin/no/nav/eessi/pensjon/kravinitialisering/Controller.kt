package no.nav.eessi.pensjon.kravinitialisering

import no.nav.eessi.pensjon.kravinitialisering.behandlehendelse.BehandleHendelseKlient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

//Test midlertidig
@RestController
class Controller(private val hendelseKlient: BehandleHendelseKlient) {

    @PostMapping("kravinit", consumes = ["application/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prefillDocument(@RequestBody model: BehandleHendelseModel) {

        if (model.hendelsesKode == HendelseKode.SOKNAD_OM_ALDERSPENSJON) {
            hendelseKlient.kallOpprettBehandleHendelse(model)
        }

    }

}