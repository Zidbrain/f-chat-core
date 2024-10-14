package io.github.zidbrain.routing

import io.github.zidbrain.exception.UnauthorizedException
import io.github.zidbrain.service.AuthService
import io.github.zidbrain.service.IdTokenParser
import io.github.zidbrain.service.TokenService
import io.github.zidbrain.service.model.UserRefreshTokenInfo
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Routing.auth() {
    val tokenParser by inject<IdTokenParser>()
    val authService by inject<AuthService>()
    val tokenService by inject<TokenService>()
    route("/auth") {
        post("/getRefreshToken") { request: GetRefreshTokenRequestDto ->
            val email = tokenParser.parseToken(request.idToken)
            val userInfo = authService.getOrCreateRefreshToken(email, request.devicePublicKey)
            call.respond(userInfo.toDto())
        }
        post("/getAccessToken") { request: GetAccessTokenRequestDto ->
            val info = tokenService.decodeRefreshToken(request.refreshToken) ?: throw UnauthorizedException("Refresh token is wrong or expired")
            if (!authService.verifyDeviceInfo(info.userId, info.deviceId)) throw UnauthorizedException("Unable to verify device id")

            val token = tokenService.issueAccessToken(info)
            call.respond(GetAccessTokenResponseDto(token))
        }
    }
}

@Serializable
data class GetRefreshTokenRequestDto(val idToken: String, val devicePublicKey: String)

@Serializable
data class GetRefreshTokenResponseDto(val refreshToken: String, val userId: String)

@Serializable
data class GetAccessTokenRequestDto(val refreshToken: String)

@Serializable
data class GetAccessTokenResponseDto(val accessToken: String)

private fun UserRefreshTokenInfo.toDto() = GetRefreshTokenResponseDto(refreshToken, userId)