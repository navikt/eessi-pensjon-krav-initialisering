package no.nav.eessi.pensjon

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@SpringBootApplication
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = false)
class EessiPensjonKravInitialiseringApplication

fun main(args: Array<String>) {
	runApplication<EessiPensjonKravInitialiseringApplication>(*args)
}
