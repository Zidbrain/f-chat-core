package io.github.zidbrain.service.model

import io.ktor.server.auth.*

data class SessionInfo(
    val userId: String,
    val deviceId: String
) : Principal