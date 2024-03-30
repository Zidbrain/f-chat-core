package io.github.zidbrain.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
}

data class UserSession(val state: String, val token: String) : Principal