package io.github.zidbrain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.github.zidbrain.service.model.SessionInfo
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Single
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

    fun issueRefreshToken(userId: String, deviceId: String): String = jwtBuilder
        .withClaim("userId", userId)
        .withClaim("deviceId", deviceId)
        .withAudience("refresh")
        .sign()

    fun verifyRefreshToken(token: String, userId: String, deviceId: String): Boolean {
        val verifier = jwtVerifier("refresh")
            .withClaim("userId", userId)
            .withClaim("deviceId", deviceId)
            .build()
        return try {
            verifier.verify(token)
            true
        }
        catch (ex: Exception) {
            false
        }
    }

    fun decodeRefreshToken(token: String): SessionInfo? {
        val verifier = jwtVerifier("refresh").build()
        return try {
            val jwt = verifier.verify(token)
            SessionInfo(jwt.getClaim("userId").asString(), jwt.getClaim("deviceId").asString())
        } catch (ex: JWTVerificationException) {
            null
        }
    }

    fun issueAccessToken(info: SessionInfo): String {
        return jwtBuilder
            .withAudience("access")
            .withClaim("userId", info.userId)
            .withClaim("deviceId", info.deviceId)
            .withExpiresAt(OffsetDateTime.now().plusHours(2).toInstant())
            .sign()
    }

    fun configureJwtAuthentication(config: AuthenticationConfig) = with(config) {
        jwt {
            realm = jwtRealm
            verifier(jwtVerifier("access").build())
            validate {
                SessionInfo(it["userId"]!!, it["deviceId"]!!)
            }
        }
    }

    fun verifyAccessToken(token: String): SessionInfo? {
        val verifier = jwtVerifier("access").build()
        return try {
            val jwt = verifier.verify(token)
            SessionInfo(jwt.getClaim("userId").asString(), jwt.getClaim("deviceId").asString())
        } catch (ex: JWTVerificationException) {
            null
        }
    }
}