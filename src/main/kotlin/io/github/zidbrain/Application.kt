package io.github.zidbrain

import io.github.zidbrain.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureKoin()
        configureApplication()
    }.start(wait = true)
}

fun Application.configureApplication() {
    configureDatabaseMigration()
    configureStatusPages()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    configureScheduling()
}
