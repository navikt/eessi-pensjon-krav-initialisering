package no.nav.eessi.pensjon.eux

import no.nav.eessi.pensjon.ResourceHelper
import no.nav.eessi.pensjon.json.mapAnyToJson
import no.nav.eessi.pensjon.json.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class BucMetadataSerDesTest{
    @Test
    fun `Sjekker at serialisering virker`() {
        val model = BucMetadata(
            listOf(Document("", "2020-12-08T09:52:55.345+0000",
                listOf(Conversation(
                    listOf(Participant("Receiver", Organisation("SE")
                    )))), listOf(Version("1")))
            ),
            BucType.P_BUC_01,
            "2020-11-08T09:52:55.345+0000")

        val serialized = model.toJson()
        val result = BucMetadata.fromJson(serialized)

        JSONAssert.assertEquals(serialized, result.toJson(),  JSONCompareMode.LENIENT)
    }

    @Test
    fun `Sjekker at deserialisering gir riktig verdi`() {
        val buc = ResourceHelper.getResourceBucMetadata("buc/bucMedP2000.json").toJson()
        val model = BucMetadata.fromJson(buc)
        val result = mapAnyToJson(model)

        JSONAssert.assertEquals(buc, result, JSONCompareMode.LENIENT)
        assertEquals(
            model.documents[0]
                .conversations[0]
                .participants?.get(0)
                ?.organisation?.countryCode,
            "NO"
        )
    }
}