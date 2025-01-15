package io.github.zidbrain.plugins

import io.github.zidbrain.routing.auth
import io.github.zidbrain.routing.chat
import io.github.zidbrain.routing.user
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        auth()
        user()
        chat()
    }
}
