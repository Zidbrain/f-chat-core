package io.github.zidbrain.util

import io.github.zidbrain.model.AuthenticatedDeviceInfo
import io.github.zidbrain.service.model.SessionInfo
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun RoutingContext.getAuthenticatedDeviceInfo(): AuthenticatedDeviceInfo {
    val principal = call.principal<SessionInfo>()!!
    return AuthenticatedDeviceInfo(
        deviceId = principal.deviceId,
        userId = principal.userId
    )
}