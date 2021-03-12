package no.nav.eessi.pensjon.kravinitialisering

data class BehandleHendelseModel(
    var sakId: String,
    var bucId: String,
    var hendelsesKode: HendelseKode,
    var beskrivelse: String? = null
)

enum class HendelseKode {
    SOKNAD_OM_ALDERSPENSJON,
    SOKNAD_OM_UFORE,
    INFORMASJON_FRA_UTLANDET,
    UKJENT
}