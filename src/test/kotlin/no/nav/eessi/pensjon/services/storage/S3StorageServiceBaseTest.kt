package no.nav.eessi.pensjon.services.storage

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import no.nav.eessi.pensjon.s3.S3StorageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import java.net.ServerSocket

open class S3StorageServiceBaseTest {

    lateinit var s3StorageServiceService: S3StorageService
    lateinit var s3MockClient: AmazonS3
    lateinit var s3api: S3Mock

    @BeforeEach fun setup() {
        val s3Port = randomOpenPort()
        s3api = S3Mock.Builder().withPort(s3Port).withInMemoryBackend().build()
        s3api.start()

        val endpoint = AwsClientBuilder.EndpointConfiguration("http://localhost:$s3Port", "us-east-1")

        val s3Client = AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(true)
                .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
                .withEndpointConfiguration(endpoint)
                .build()
        s3Client.createBucket("eessipensjon")

        s3MockClient = Mockito.spy(s3Client)

        s3StorageServiceService =  Mockito.spy(S3StorageService(s3MockClient))
        s3StorageServiceService.bucketname = "eessipensjon"
        s3StorageServiceService.env = "q1"
        //s3storageService.passphrase = "something very vey tricky to hack"
        s3StorageServiceService.init()

/*        storageController = Mockito.spy(StorageController(s3storageService, SpringTokenValidationContextHolder()))
        storageController.initMetrics()*/
    }

    @AfterEach fun teardown() {
        s3api.stop()
    }

    fun randomOpenPort(): Int = ServerSocket(0).use { it.localPort }

}
