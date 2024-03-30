package io.github.zidbrain.routing

import io.github.zidbrain.model.User
import io.github.zidbrain.service.UserService
import io.github.zidbrain.util.getAuthenticatedDeviceInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Routing.user() = route("/user") {
    authenticate {
        val userService by inject<UserService>()
        get("/contacts") {
            val device = getAuthenticatedDeviceInfo()
            val contacts = userService.getContactsFor(device.userId)
            call.respond(
                GetContactsResponseDto(
                    users = contacts.map { it.toDto() }
                )
            )
        }
        get("/search") {
            val searchString = call.request.queryParameters["searchString"]
            val users = searchString?.let { search -> userService.searchUsers(search).map { it.toDto() } } ?: emptyList()
            call.respond(GetContactsResponseDto(users))
        }
        post("/addContact/{contactId}") {
            val contactId = call.parameters["contactId"]!!
            val info = getAuthenticatedDeviceInfo()
            userService.addContact(info.userId, contactId)
            call.respond(HttpStatusCode.Created)
        }
    }
}

@Serializable
private data class GetContactsResponseDto(val users: List<UserDto>)

@Serializable
private data class UserDto(val id: String, val email: String, val displayName: String)

private fun User.toDto() = UserDto(
    id = id,
    email = email,
    displayName = displayName
)