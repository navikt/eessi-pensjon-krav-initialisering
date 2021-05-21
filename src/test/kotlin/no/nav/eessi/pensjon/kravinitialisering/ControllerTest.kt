package no.nav.eessi.pensjon.kravinitialisering

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import io.mockk.mockk
import no.nav.eessi.pensjon.json.toJson
import no.nav.eessi.pensjon.kravinitialisering.services.LagringsService
import no.nav.eessi.pensjon.s3.S3StorageService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.time.LocalDateTime

internal class ControllerTest {

    private lateinit var controller: Controller

    private lateinit var lagringsService: LagringsService
    private lateinit var s3StorageService: S3StorageService
    private lateinit var s3MockClient: AmazonS3
    private lateinit var s3api: S3Mock

    @BeforeEach
    fun setup() {
        val s3Port = randomOpenPort()

        s3api = S3Mock.Builder().withPort(s3Port).withInMemoryBackend().build()
        s3api.start()
        val endpoint = AwsClientBuilder.EndpointConfiguration("http://localhost:$s3Port", "us-east-1")

        s3MockClient = AmazonS3ClientBuilder.standard()
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
            .withEndpointConfiguration(endpoint)
            .build()

        s3MockClient.createBucket("eessipensjon")
        s3StorageService = S3StorageService(s3MockClient)
        s3StorageService.bucketname = "eessipensjon"
        s3StorageService.env = "localtestarea"
        s3StorageService.init()
        lagringsService = LagringsService(s3StorageService)
        controller = Controller(mockk(), s3StorageService)
    }

    @Test
    fun listAllPBuc() {
        lagreHendelseSoknadOmUfore("1", LocalDateTime.of(2020, 1, 1, 12, 0, 0))
        lagreHendelseSoknadOmUfore("2", LocalDateTime.of(2020, 1, 1, 11, 0, 0))
        println(controller.listAllPBuc())
    }

    fun lagreHendelseSoknadOmUfore(saksId: String, timestamp: LocalDateTime): BehandleHendelseModel {

        val hendelse = BehandleHendelseModel(
            saksId,
            "654654351",
            HendelseKode.SOKNAD_OM_UFORE,
            timestamp
        )

        val path = lagringsService.hentPathMedSakId(hendelse)
        s3StorageService.put(path, hendelse.toJson())

        return hendelse
    }

    fun randomOpenPort(): Int = ServerSocket(0).use { it.localPort }
}