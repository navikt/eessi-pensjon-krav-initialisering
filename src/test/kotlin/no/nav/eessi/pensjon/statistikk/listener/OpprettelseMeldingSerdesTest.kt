package no.nav.eessi.pensjon.statistikk.listener

import no.nav.eessi.pensjon.json.mapAnyToJson
import no.nav.eessi.pensjon.json.mapJsonToAny
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.json.typeRefs
import no.nav.eessi.pensjon.statistikk.models.OpprettelseType
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class OpprettelseMeldingSerdesTest {
    @Test
    fun `Sjekker at serialisering virker`() {
        val model = OpprettelseMelding(opprettelseType = OpprettelseType.BUC, rinaId =  "1208875", dokumentId = "djksdfsdl3435kj3452", vedtaksId = null)
        val serialized = model.toJson()

        val result = mapJsonToAny(serialized, typeRefs<OpprettelseMelding>())

        JSONAssert.assertEquals(serialized, result.toJson(), JSONCompareMode.LENIENT)
    }

    @Test
    fun `Sjekker at deserialisering gir riktig verdi`() {
        val json = """{
              "opprettelseType" : "BUC",
              "rinaId" : "1208875",
              "dokumentId" : "32456365464564"
        }""".trimMargin()

        val model = mapJsonToAny(json, typeRefs<OpprettelseMelding>())


        val result = mapAnyToJson(model)
        JSONAssert.assertEquals(json, result, JSONCompareMode.LENIENT)
    }
}