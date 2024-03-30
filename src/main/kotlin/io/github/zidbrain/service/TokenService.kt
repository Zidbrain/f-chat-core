package io.github.zidbrain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.time.OffsetDateTime

class TokenService(secretService: SecretService) {

    private val jwtDomain = "https://zidbrain.github.io/"
    private val jwtSecret = secretService.getSecret("jwtSecret")
    private val jwtRealm = "F Chat"

    private val jwtBuilder = JWT.create()
        .withIssuer(jwtDomain)
        .withExpiresAt(OffsetDateTime.now().plusMonths(1).toInstant())

    private fun JWTCreator.Builder.sign() = sign(Algorithm.HMAC256(jwtSecret))

    private fun jwtVerifier(aud: String) = JWT
        .require(Algorithm.HMAC256(jwtSecret))
        .withAudience(aud)
        .withIssuer(jwtDomain)

    fun issueRefreshToken(userId: String, devicePublicKey: String): String = jwtBuilder
        .withClaim("userId", userId)
        .withClaim("devicePublicKey", devicePublicKey)
        .withAudience("refresh")
        .sign()

    fun verifyRefreshToken(token: String, userId: String, devicePublicKey: String): Boolean {
        val verifier = jwtVerifier("refresh")
            .withClaim("userId", userId)
            .withClaim("devicePublicKey", devicePublicKey)
            .build()
        return try {
            verifier.verify(token)
            true
        }
        catch (ex: Exception) {
            false
        }
    }

    fun issueAccessToken(refreshToken: String): String {
        val verifier = jwtVerifier("refresh")
            .build()
        val parsedToken = verifier.verify(refreshToken)
        return jwtBuilder
            .withAudience("access")
            .withClaim("userId", parsedToken.getClaim("userId").asString())
            .withClaim("devicePublicKey", parsedToken.getClaim("devicePublicKey").asString())
            .withExpiresAt(OffsetDateTime.now().plusHours(2).toInstant())
            .sign()
    }

    fun configureJwtAuthentication(config: AuthenticationConfig) = with(config) {
        jwt {
            realm = jwtRealm
            verifier(jwtVerifier("access").build())
            validate { JWTPrincipal(it.payload) }
        }
    }
}