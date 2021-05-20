package no.nav.eessi.pensjon.kravinitialisering.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import no.nav.eessi.pensjon.kravinitialisering.BehandleHendelseModel
import no.nav.eessi.pensjon.kravinitialisering.HendelseKode
import no.nav.eessi.pensjon.s3.S3StorageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.time.LocalDateTime

internal class LagringsServiceTest {

    private lateinit var storageService: S3StorageService
    private lateinit var lagringsService: LagringsService
    private lateinit var s3MockClient: AmazonS3
    private lateinit var s3api: S3Mock

    @BeforeEach
    fun setup() {
        val s3Port = ServerSocket(0).use { it.localPort }

        s3api = S3Mock.Builder().withPort(s3Port).withInMemoryBackend().build()
        s3api.start()
        val endpoint = AwsClientBuilder.EndpointConfiguration("http://localhost:$s3Port", "us-east-1")

        s3MockClient = AmazonS3ClientBuilder.standard()
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
            .withEndpointConfiguration(endpoint)
            .build()

        s3MockClient.createBucket("eessipensjon")
        storageService = S3StorageService(s3MockClient)
        storageService.bucketname = "eessipensjon"
        storageService.env = "q1"
        storageService.init()

        lagringsService = LagringsService(storageService)
    }

    @AfterEach
    fun teardown() {
        s3api.stop()
    }

    @Test
    fun `lagre og hente P2200 hendelse`() {
        val hendelse = mockHendelse("54321", HendelseKode.SOKNAD_OM_UFORE)

        lagringsService.lagreHendelseMedSakId(hendelse)

        val result = lagringsService.hentHendelse(hendelse)
        assertEquals(hendelse, result)
    }

    fun mockHendelse(bucId: String, hendelsekode: HendelseKode): BehandleHendelseModel {
        return BehandleHendelseModel("12354", bucId, hendelsekode, beskrivelse = "beskrivelse", opprettetDato = LocalDateTime.of(2020, 1, 1, 10, 10, 10))
    }
    fun mockHendelseUtenDato(bucId: String, hendelsekode: HendelseKode): BehandleHendelseModel {
        return BehandleHendelseModel("12354", bucId, hendelsekode, beskrivelse = "beskrivelse", opprettetDato = LocalDateTime.of(2020, 1, 1, 10, 10, 10))
    }

}