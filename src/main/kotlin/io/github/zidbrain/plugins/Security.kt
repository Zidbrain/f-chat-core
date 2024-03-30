package io.github.zidbrain.plugins

import io.github.zidbrain.service.TokenService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val tokenService by inject<TokenService>()
    authentication {
        tokenService.configureJwtAuthentication(this)
    }
}
