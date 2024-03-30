package io.github.zidbrain.routing

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import io.github.zidbrain.service.TokenService
import io.github.zidbrain.service.AuthService
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject


fun Routing.auth() {
    val verifier by inject<GoogleIdTokenVerifier>()
    val authService by inject<AuthService>()
    val tokenService by inject<TokenService>()
    route("/auth") {
        post("/getRefreshToken") { request: GetRefreshTokenRequestDto ->
            val token = verifier.verify(request.idToken)?.payload
                ?: throw BadRequestException("Failed to get payload from token")

            val refreshToken = authService.getOrCreateRefreshToken(token.email, request.devicePublicKey)
            call.respond(GetRefreshTokenResponseDto(refreshToken))
        }
        post("/getAccessToken") { request: GetAccessTokenRequestDto ->
            val token = tokenService.issueAccessToken(request.refreshToken)
            call.respond(GetAccessTokenResponseDto(token))
        }
    }
}

@Serializable
private data class GetRefreshTokenRequestDto(val idToken: String, val devicePublicKey: String)

@Serializable
private data class GetRefreshTokenResponseDto(val refreshToken: String)

@Serializable
private data class GetAccessTokenRequestDto(val refreshToken: String)

@Serializable
private data class GetAccessTokenResponseDto(val accessToken: String)