package no.nav.eessi.pensjon.kravinitialisering

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.eessi.pensjon.json.JsonDateDeserializer
import no.nav.eessi.pensjon.json.JsonDateSerializer
import java.time.LocalDateTime


data class BehandleHendelseModel(
    val sakId: String? = null,
    val bucId: String? = null,
    val hendelsesKode: HendelseKode,
    @JsonDeserialize(using = JsonDateDeserializer::class)
    @JsonSerialize(using = JsonDateSerializer::class)
    val opprettetDato: LocalDateTime = LocalDateTime.now(),
    val beskrivelse: String? = null
)

enum class HendelseKode {
    SOKNAD_OM_ALDERSPENSJON,
    SOKNAD_OM_UFORE,
    INFORMASJON_FRA_UTLANDET,
    UKJENT
}



