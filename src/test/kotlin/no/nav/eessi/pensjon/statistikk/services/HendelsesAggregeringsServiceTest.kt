package no.nav.eessi.pensjon.statistikk.services

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.ResourceHelper
import no.nav.eessi.pensjon.eux.BucMetadata
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.Sed
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.s3.S3StorageService
import no.nav.eessi.pensjon.statistikk.models.HendelseType
import no.nav.eessi.pensjon.statistikk.models.SedMeldingP6000Ut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelsesAggregeringsServiceTest {

    private var euxService = mockk<EuxService>(relaxed = true)
    private var s3Service = mockk<S3StorageService>(relaxed = true)
    private var infoService = HendelsesAggregeringsService(euxService,  s3Service)

    @Test
    fun `gitt en sedOpprettet melding så populer sedOpprettetMeldingUt`() {
        val pesysSaksID = "22929783"
        val dokumentId = "d740047e730f475aa34ae59f62e3bb99"
        val rinaid = "111"
        val vedtaksId = "333"
        val mottakerland = listOf("NO")

        //Mocker BUC
        val bucJson = ResourceHelper.getResourceBucMetadata("buc/bucMedP2000.json").toJson()
        val buc = BucMetadata.fromJson(bucJson)
        every { euxService.getBucMetadata(any())} returns buc

        // Mocker SED
        val p6000Json = ResourceHelper.getResourceSed("sed/P2000-preutfylt-fnr-og-sakid.json").toJson()
        val p6000 = Sed.fromJson(p6000Json)
        every { euxService.getSed(any(), any()) } returns p6000

        val sedOpprettetMeldingUt = infoService.aggregateSedOpprettetData(rinaid, dokumentId, vedtaksId)

        assertEquals(sedOpprettetMeldingUt?.rinaid, rinaid)
        assertEquals(sedOpprettetMeldingUt?.dokumentId, dokumentId)
        assertEquals(sedOpprettetMeldingUt?.pesysSakId, pesysSaksID)
        assertEquals(sedOpprettetMeldingUt?.opprettetTidspunkt, "2020-12-08T09:53:36.241Z")
        assertEquals(sedOpprettetMeldingUt?.vedtaksId, vedtaksId)
        assertEquals(sedOpprettetMeldingUt?.mottakerLand, mottakerland)
        assertEquals(sedOpprettetMeldingUt?.rinaDokumentVersjon, "2")
    }

    @Test
    fun `gitt en sedOpprettet for P6000 melding så populer sedOpprettetMeldingUt`() {
        //Mocker BUC
        val bucJson = ResourceHelper.getResourceBucMetadata("buc/bucMedP6000.json").toJson()
        val buc = BucMetadata.fromJson(bucJson)
        every { euxService.getBucMetadata(any())} returns buc

        // Mocker SED
        val p6000Json = ResourceHelper.getResourceSed("sed/P6000-komplett.json").toJson()
        val p6000 = Sed.fromJson(p6000Json)
        every { euxService.getSed(any(), any()) } returns p6000

        val dokumentId = "08e5310500a94640abfb309e481ca319"
        val rinaId = "1271728"
        val mottakerland = listOf("NO")

        val sedOpprettetMeldingUt = infoService.aggregateSedOpprettetData(rinaId, dokumentId, null)

        assertEquals(sedOpprettetMeldingUt?.rinaid, rinaId)
        assertEquals(sedOpprettetMeldingUt?.dokumentId, dokumentId)
        assertEquals(sedOpprettetMeldingUt?.pesysSakId, "22919968")
        assertEquals(sedOpprettetMeldingUt?.opprettetTidspunkt, "2021-02-11T13:08:29.914Z")
        assertEquals(sedOpprettetMeldingUt?.vedtaksId, null)
        assertEquals(sedOpprettetMeldingUt?.mottakerLand, mottakerland)
        assertEquals(sedOpprettetMeldingUt?.rinaDokumentVersjon, "5")

    }

    @Test
    fun `gitt en sed sendt for P6000 melding så populer SedMeldingUt `() {
        //Mocker BUC
        val bucJson = ResourceHelper.getResourceBucMetadata("buc/bucMedP6000.json").toJson()
        val buc = BucMetadata.fromJson(bucJson)
        every { euxService.getBucMetadata(any())} returns buc

        // Mocker SED
        val p6000Json = ResourceHelper.getResourceSed("sed/P6000-komplett.json").toJson()
        val p6000 = Sed.fromJson(p6000Json)
        every { euxService.getSed(any(), any()) } returns p6000

        val dokumentId = "08e5310500a94640abfb309e481ca319"
        val rinaId = "1271728"
        val mottakerland = listOf("NO")

        val sedOpprettetMeldingUt = infoService.populerSedMeldingUt(rinaId, dokumentId, null, HendelseType.SED_SENDT)
            as SedMeldingP6000Ut

        assertEquals(sedOpprettetMeldingUt.rinaid, rinaId)
        assertEquals(sedOpprettetMeldingUt.dokumentId, dokumentId)
        assertEquals(sedOpprettetMeldingUt.pesysSakId, "22919968")
        assertEquals(sedOpprettetMeldingUt.opprettetTidspunkt, "2021-02-11T13:08:29.914Z")
        assertEquals(sedOpprettetMeldingUt.vedtaksId, null)
        assertEquals(sedOpprettetMeldingUt.mottakerLand, mottakerland)
        assertEquals(sedOpprettetMeldingUt.rinaDokumentVersjon, "5")

        //SedMeldingP6000Ut
        assertEquals(sedOpprettetMeldingUt.bostedsland, "HR")
        assertEquals(sedOpprettetMeldingUt.bruttoBelop, "12482")
        assertEquals(sedOpprettetMeldingUt.nettoBelop, "10000")
        assertEquals(sedOpprettetMeldingUt.valuta, "NOK")
        assertEquals(sedOpprettetMeldingUt.anmodningOmRevurdering, "1")
        assertEquals(sedOpprettetMeldingUt.pensjonsType, "03")
        assertEquals(sedOpprettetMeldingUt.vedtakStatus, "04")
    }
}