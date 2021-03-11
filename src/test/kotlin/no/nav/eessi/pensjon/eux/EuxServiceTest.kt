package no.nav.eessi.pensjon.eux

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.ResourceHelper
import no.nav.eessi.pensjon.ResourceHelper.Companion.getResourceBucMetadata
import no.nav.eessi.pensjon.ResourceHelper.Companion.getResourceSed
import no.nav.eessi.pensjon.s3.S3StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
internal class EuxServiceTest {

    @Mock
    lateinit var euxOidcRestTemplate: RestTemplate

    @Mock
    lateinit var s3StorageService: S3StorageService

    lateinit var euxService: EuxService

    @BeforeEach
    fun before() {
        euxService = EuxService(EuxKlient(euxOidcRestTemplate))
    }

    @Test
    fun `Se timestamp konverters fra zone til offsettDateTime`() {
        val gyldigBuc : BucMetadata = getResourceBucMetadata("buc/bucMedP2000.json")
        val mockEuxRinaid = "123456"
        val mockEuxDocumentId = "d740047e730f475aa34ae59f62e3bb99"

        doReturn(gyldigBuc)
            .whenever(euxOidcRestTemplate).getForObject(
                eq("/buc/$mockEuxRinaid"),
                eq(BucMetadata::class.java))

        val metaData = euxService.getBucMetadata(mockEuxRinaid)
        val offsetDateTime =
            metaData?.let { HendelsesAggregeringsService(euxService, s3StorageService).getTimeStampFromSedMetaDataInBuc(it, mockEuxDocumentId) }

        assertEquals("2020-12-08T09:53:36.241Z", offsetDateTime)
     }

    @Test
    fun `Gitt en SED når norskSakId er utfylt så returner norsk saksId`() {
        val gyldigBuc : Sed = getResourceSed("sed/P2000-minimal-med-en-norsk-sakId.json")
        val mockEuxRinaid = "123456"
        val mockEuxDocumentId = "d740047e730f475aa34ae59f62e3bb99"

        doReturn(gyldigBuc)
            .whenever(euxOidcRestTemplate).getForObject(
                eq("/buc/$mockEuxRinaid/sed/$mockEuxDocumentId"),
                eq(Sed::class.java))

        val sed = euxService.getSed(mockEuxRinaid, mockEuxDocumentId)
        val saksNummer = hentSaksNummer(sed)

        assertEquals("123456", saksNummer)
    }

    @Test
    fun `Gitt en SED når norskSakId ikke er utfylt så returner null`() {
        val gyldigBuc : Sed = getResourceSed("sed/P2000-minimal-med-kun-utenlandsk-sakId.json")
        val mockEuxRinaid = "123456"
        val mockEuxDocumentId = "d740047e730f475aa34ae59f62e3bb99"

        doReturn(gyldigBuc)
            .whenever(euxOidcRestTemplate).getForObject(
                eq("/buc/$mockEuxRinaid/sed/$mockEuxDocumentId"),
                eq(Sed::class.java))

        val sed = euxService.getSed(mockEuxRinaid, mockEuxDocumentId)
        val sakId = hentSaksNummer(sed)

        assertEquals(null, sakId)
    }

    private fun hentSaksNummer(sed: Sed?) =
        sed?.nav?.eessisak?.firstOrNull { sak -> sak?.land == "NO" }?.saksnummer

    @Test
    fun `Gitt en SED når ingen sakId er utfylt så returner null`() {
        val gyldigBuc : Sed = ResourceHelper.getResourceSed("sed/P2000-minimal-uten-sakId.json")
        val mockEuxRinaid = "123456"
        val mockEuxDocumentId = "d740047e730f475aa34ae59f62e3bb99"

        doReturn(gyldigBuc)
            .whenever(euxOidcRestTemplate).getForObject(
                eq("/buc/$mockEuxRinaid/sed/$mockEuxDocumentId"),
                eq(Sed::class.java))

        val sed = euxService.getSed(mockEuxRinaid, mockEuxDocumentId)
        val sakId = hentSaksNummer(sed)

        assertEquals(null, sakId)
    }
}


