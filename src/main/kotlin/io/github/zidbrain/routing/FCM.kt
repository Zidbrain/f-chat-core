package io.github.zidbrain.routing

import io.github.zidbrain.service.FCMService
import io.github.zidbrain.util.getAuthenticatedDeviceInfo
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Routing.fcm() = route("/fcm") {
    authenticate {
        post("/update-token") { request: UpdateFCMTokenRequest ->
            val fcmService by inject<FCMService>()
            val deviceId = getAuthenticatedDeviceInfo().deviceId
            fcmService.updateToken(deviceId, request.token)
            call.response.status(HttpStatusCode.Accepted)
        }
    }
}

@Serializable
private data class UpdateFCMTokenRequest(val token: String)