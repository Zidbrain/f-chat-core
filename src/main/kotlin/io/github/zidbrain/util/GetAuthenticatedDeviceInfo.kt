package io.github.zidbrain.util

import io.github.zidbrain.model.AuthenticatedDeviceInfo
import io.github.zidbrain.service.model.SessionInfo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*

fun PipelineContext<Unit, ApplicationCall>.getAuthenticatedDeviceInfo(): AuthenticatedDeviceInfo {
    val principal = call.principal<SessionInfo>()!!
    return AuthenticatedDeviceInfo(
        deviceId = principal.deviceId,
        userId = principal.userId
    )
}