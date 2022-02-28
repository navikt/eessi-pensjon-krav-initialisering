package no.nav.eessi.pensjon.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = LoggerFactory.getLogger(S3StorageConfig::class.java)

@Configuration
class S3StorageConfig {

    @Value("\${GCP_ACCESS_KEY}")
    lateinit var accessKey: String

    @Value("\${GCP_ACCESS_SECRET}")
    lateinit var secretKey: String

    @Value("\${GCP_STORAGE_API_URL}")
    lateinit var gcpStorageApiUrl: String

    @Bean
    fun s3(): AmazonS3 {
        logger.info("Creating AmazonS3 standard client with endpoint: $gcpStorageApiUrl")
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(gcpStorageApiUrl, "auto"))
            .enablePathStyleAccess()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .build()
    }
}
