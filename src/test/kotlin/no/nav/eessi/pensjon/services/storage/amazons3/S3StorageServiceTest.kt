package no.nav.eessi.pensjon.services.storage.amazons3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import no.nav.eessi.pensjon.s3.S3StorageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.net.ServerSocket
import java.time.LocalDateTime


@ExtendWith(MockitoExtension::class)
class S3StorageServiceTest {

    private lateinit var storageService: S3StorageService
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
        storageService = S3StorageService(s3MockClient)
        storageService.bucketname = "eessipensjon"
        storageService.env = "q1"
        //storage.passphrase = "mypassphrase"
        storageService.init()
    }

    @AfterEach
    fun teardown() {
        s3api.stop()
    }

    @Test
    fun `add files in different directories and list them all`() {
        val aktoerId1 = "14725802541"
        val p2000Directory = "P2000"
        val p4000Directory = "P4000"

        val p2000value = "Final P2000-document"
        val p4000value = "Final P4000-document"

        storageService.put(aktoerId1 + "___" + "$p2000Directory/${LocalDateTime.now()}/document.txt", p2000value)
        storageService.put(aktoerId1 + "___" + "$p4000Directory/${LocalDateTime.now()}/document.txt", p4000value)

        val fileListAktoer1 = storageService.list(aktoerId1)
        assertEquals(2, fileListAktoer1.size)

        val aktoerId2 = "25896302020"

        storageService.put(aktoerId2 + "___" + "$p2000Directory/${LocalDateTime.now()}/document.txt", p2000value)
        storageService.put(aktoerId2 + "___" + "$p4000Directory/${LocalDateTime.now()}/document.txt", p4000value)

        val fileListAktoer2 = storageService.list(aktoerId2)
        assertEquals(2, fileListAktoer2.size)
    }

    @Test
    fun `add multiple files and list them`() {
        val directory = "P4000"
        val aktoerId = "12345678910"

        val value1 = "First draft"
        val timestamp1 = LocalDateTime.now().minusHours(5)

        storageService.put(aktoerId + "___" + "$directory/$timestamp1/document.txt", value1)

        val value2 = "Second draft"
        val timestamp2 = LocalDateTime.now().minusHours(2)
        storageService.put(aktoerId + "___" + "$directory/$timestamp2/document.txt", value2)

        val value3 = "Final document"
        val timestamp3 = LocalDateTime.now()
        storageService.put(aktoerId + "___" + "$directory/$timestamp3/document.txt", value3)

        val fileList = storageService.list(aktoerId + "___" + directory)
        assertEquals(3, fileList.size)

        val fetchtedValue1 = storageService.get(fileList[0])
        val fetchtedValue2 = storageService.get(fileList[1])
        val fetchtedValue3 = storageService.get(fileList[2])

        assertEquals(value1, fetchtedValue1)
        assertEquals(value2, fetchtedValue2)
        assertEquals(value3, fetchtedValue3)
    }

    @Test
    fun `put file into bucket, list it, read it back and finally delete it`() {
        val directory = "P2000"
        val aktoerId = "12435678910"
        val value = "A string that has to be persisted.\nAnd this line too."

        storageService.put(aktoerId + "___" + "$directory/testfile.txt", value)

        val fileList = storageService.list(aktoerId + "___" + directory)
        assertEquals(1, fileList.size, "Expect that 1 entry is returned")

        val fetchedValue = storageService.get(fileList[0])
        assertEquals(value, fetchedValue, "The stored and fetched values should be equal")

        storageService.delete(fileList[0])

        val fileListAfterDelete = storageService.list(aktoerId + "___" + directory)
        assertEquals(0, fileListAfterDelete.size, "Expect that 0 entries are returned")
    }

    @Test
    fun `Given a logged in saksbehandler when listing in S3 then allow listing files for all citizens`() {
        val aktoerId1 = "12345678910"
        val aktoerId2 = "12345678911"

        val p2000value = "Final P2000-document"
        val p4000value = "Final P4000-document"

        storageService.put(aktoerId1 + "___" + "${LocalDateTime.now()}/document.txt", p2000value)
        storageService.put(aktoerId2 + "___" + "${LocalDateTime.now()}/document.txt", p4000value)

        val fileListAktoer1 = storageService.list(aktoerId2)
        assertEquals(1, fileListAktoer1.size)
    }

    fun randomOpenPort(): Int = ServerSocket(0).use { it.localPort }
}
