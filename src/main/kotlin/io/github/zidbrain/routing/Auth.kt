package io.github.zidbrain.routing

import io.github.zidbrain.exception.UnauthorizedException
import io.github.zidbrain.service.AuthService
import io.github.zidbrain.service.TokenService
import io.github.zidbrain.service.model.UserRefreshTokenInfo
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.koin.ktor.ext.inject

fun Routing.auth() {
    val authService by inject<AuthService>()
    val tokenService by inject<TokenService>()
    route("/auth") {
        post("/getRefreshToken") { request: GetRefreshTokenRequestDto ->
            val userInfo = when (val data = request.data) {
                is GetRefreshTokenRequestDtoData.GoogleSSO -> authService.getGoogleSSORefreshToken(
                    idToken = data.idToken,
                    devicePublicKey = data.devicePublicKey
                )

                is GetRefreshTokenRequestDtoData.PasswordAuth -> authService.getPasswordAuthRefreshToken(
                    email = data.email,
                    password = data.password,
                    devicePublicKey = data.devicePublicKey
                )
            }
            call.respond(userInfo.toDto())
        }
        post("/getAccessToken") { request: GetAccessTokenRequestDto ->
            val info = tokenService.decodeRefreshToken(request.refreshToken)
                ?: throw UnauthorizedException("Refresh token is wrong or expired")
            if (!authService.verifyDeviceInfo(
                    info.userId,
                    info.deviceId
                )
            ) throw UnauthorizedException("Unable to verify device id")

            val token = tokenService.issueAccessToken(info)
            call.respond(GetAccessTokenResponseDto(token))
        }
        post("/signIn") { request: SignInRequestDto ->
            val info = authService.createUserWithPassword(request.email, request.password, request.devicePublicKey)
            call.respond(info.toDto())
        }
    }
}

@Serializable
data class SignInRequestDto(val email: String, val password: String, val devicePublicKey: String)

@Serializable
data class GetRefreshTokenRequestDto(val data: GetRefreshTokenRequestDtoData)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("authType")
sealed class GetRefreshTokenRequestDtoData {

    abstract val devicePublicKey: String

    @Serializable
    @SerialName("googleSSO")
    data class GoogleSSO(val idToken: String, override val devicePublicKey: String) : GetRefreshTokenRequestDtoData()

    @Serializable
    @SerialName("passwordAuth")
    data class PasswordAuth(val email: String, val password: String, override val devicePublicKey: String) :
        GetRefreshTokenRequestDtoData()
}

@Serializable
data class GetRefreshTokenResponseDto(val refreshToken: String, val userId: String)

@Serializable
data class GetAccessTokenRequestDto(val refreshToken: String)

@Serializable
data class GetAccessTokenResponseDto(val accessToken: String)

private fun UserRefreshTokenInfo.toDto() = GetRefreshTokenResponseDto(refreshToken, userId)