package no.nav.eessi.pensjon.config

import com.nimbusds.jwt.JWTClaimsSet
import io.micrometer.core.instrument.MeterRegistry
import no.nav.eessi.pensjon.logging.RequestIdHeaderInterceptor
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.metrics.RequestCountInterceptor
import no.nav.eessi.pensjon.shared.retry.IOExceptionRetryInterceptor
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpRequest
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.util.*


@Profile("prod", "test")
@Configuration
class RestTemplateConfiguration(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService?,
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(RestTemplateConfiguration::class.java)

    @Value("\${PEN_BEHANDLEHENDELSE_URL}")
    lateinit var penUrl: String

    /**
     * Create one RestTemplate per OAuth2 client entry to separate between different scopes per API
     */
//    @Bean
//    fun penAzureTokenRestTemplate(
//        restTemplateBuilder: RestTemplateBuilder,
//        clientConfigurationProperties: ClientConfigurationProperties,
//        oAuth2AccessTokenService: OAuth2AccessTokenService
//    ): RestTemplate? {
//        val clientProperties = Optional.ofNullable(clientConfigurationProperties.registration["proxy-credentials"])
//                .orElseThrow { RuntimeException("could not find oauth2 client config for example-onbehalfof") }
//        return restTemplateBuilder
//            .rootUri(penUrl)
//            .additionalInterceptors(
//                bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService),
//                RequestIdHeaderInterceptor(),
//                IOExceptionRetryInterceptor(),
//                RequestCountInterceptor(meterRegistry),
//                RequestResponseLoggerInterceptor()
//            )
//            .build().apply {
//                requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
//            }
//    }

    @Bean
    fun penAzureTokenRestTemplate() : RestTemplate {
        return RestTemplateBuilder()
            .rootUri(penUrl)
            .errorHandler(DefaultResponseErrorHandler())
            .additionalInterceptors(
                RequestIdHeaderInterceptor(),
                IOExceptionRetryInterceptor(),
                RequestCountInterceptor(meterRegistry),
                RequestResponseLoggerInterceptor(),
                bearerTokenInterceptor(clientProperties("pensjon-credentials"), oAuth2AccessTokenService!!)
            )
            .build().apply {
                requestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
            }
    }

    private fun clientProperties(oAuthKey: String): ClientProperties {
        return Optional.ofNullable(clientConfigurationProperties.registration[oAuthKey])
            .orElseThrow { RuntimeException("could not find oauth2 client config for example-onbehalfof") }
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            val tokenChunks = response.access_token!!.split(".")
            val tokenBody =  tokenChunks[1]
            logger.debug("subject: " + JWTClaimsSet.parse(Base64.getDecoder().decode(tokenBody).decodeToString()).subject + "/n + $response.accessToken")
            request.headers.setBearerAuth(response.access_token!!)
            execution.execute(request, body!!)
        }
    }

}