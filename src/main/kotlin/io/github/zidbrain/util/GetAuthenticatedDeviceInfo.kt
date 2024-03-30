package io.github.zidbrain.util

import io.github.zidbrain.model.DeviceInfo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*

fun PipelineContext<Unit, ApplicationCall>.getAuthenticatedDeviceInfo(): DeviceInfo {
    val principal = call.principal<JWTPrincipal>()!!
    return DeviceInfo(
        publicKey = principal["devicePublicKey"]!!,
        userId = principal["userId"]!!
    )
}