package no.nav.eessi.pensjon.eux

import no.nav.eessi.pensjon.ResourceHelper.Companion.getResourceSed
import no.nav.eessi.pensjon.json.mapAnyToJson
import no.nav.eessi.pensjon.json.toJson
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class SedSerDesTest {

    @Test
    fun `Sjekker at serialisering virker`() {
        val model = Sed(
            Nav(null, listOf(Sak("", ""))),
            sed = SedType.P2100,
            pensjon = Pensjon(
                vedtak = listOf(Vedtak(type = "02", resultat = "04", beregning = listOf(Beregning(beloepBrutto = BeloepBrutto("10000"), valuta = "NOK", beloepNetto = BeloepNetto("8000"))))),
                tilleggsinformasjon = Tilleggsinformasjon("")
            )
        )
        val serialized = model.toJson()
        val result = Sed.fromJson(serialized)

        JSONAssert.assertEquals(serialized, result.toJson(),  JSONCompareMode.LENIENT)
    }

    @Test
    fun `Sjekker at deserialisering gir riktig verdi`() {
        val sed = getResourceSed("sed/P2000-preutfylt-fnr-og-sakid.json").toJson()
        val model = Sed.fromJson(sed)
        val result = mapAnyToJson(model)

        JSONAssert.assertEquals(sed, result, JSONCompareMode.LENIENT)
    }

}