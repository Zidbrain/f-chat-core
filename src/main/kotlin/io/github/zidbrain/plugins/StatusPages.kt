package io.github.zidbrain.plugins

import com.auth0.jwt.exceptions.JWTVerificationException
import io.github.zidbrain.exception.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logError(call, cause)
            when (cause) {
                is JWTVerificationException, is UnauthorizedException -> call.respond(
                    HttpStatusCode.Unauthorized,
                    cause.message.orEmpty(),
                )

                is BadRequestException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
                is NotFoundException -> call.respond(HttpStatusCode.NotFound, cause.message ?: "")
                else -> {
                    call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}