package no.nav.eessi.pensjon.kravinitialisering

import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class BehandleHendelseModelSerdesTest {

    @Test
    fun `Serdes BehandleHendelseModel uten dato`(){
        val json = """
        {
            "sakId" : "1231231111",
            "bucId" : "3231231",
            "hendelsesKode" : "SOKNAD_OM_UFORE",
            "beskrivelse" : "Test på beskrivelsen også"
        }
        """.trimIndent()

        val hendelse = mapJsonToAny(json, typeRefs<BehandleHendelseModel>())
        assertNotNull(hendelse)
        assertNotNull(hendelse.opprettetDato)

        //sjekker at retur til json inneholder dato
        assert(hendelse.toJson().contains("opprettetDato"))
    }

    @Test
    fun `Serdes BehandleHendelseModel med dato`(){
        val json = """
        {
            "sakId" : "1231231111",
            "bucId" : "3231231",
            "hendelsesKode" : "SOKNAD_OM_UFORE",
            "beskrivelse" : "Test på beskrivelsen også",
            "opprettetDato" : "2020-01-01 12:01:02"
        }
        """.trimIndent()

        val hendelse = mapJsonToAny(json, typeRefs<BehandleHendelseModel>())
        assertEquals("2020-01-01T12:01:02", hendelse.opprettetDato.toString())
    }
}



