package io.github.zidbrain.routing

import io.github.zidbrain.model.Device
import io.github.zidbrain.service.ChatService
import io.github.zidbrain.service.ConversationService
import io.github.zidbrain.service.TokenService
import io.github.zidbrain.service.UserService
import io.github.zidbrain.service.model.SessionInfo
import io.github.zidbrain.util.OffsetDateTimeSerializer
import io.github.zidbrain.util.getAuthenticatedDeviceInfo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.koin.ktor.ext.get
import java.time.OffsetDateTime

fun Routing.chat() {
    route("/chat") {
        val chatService = get<ChatService>()

        webSocket {
            val tokenService = get<TokenService>()
            val sessionInfo = authenticate(tokenService)

            chatService.openDeviceConnection(this, sessionInfo.deviceId)
            try {
                while (true) {
                    receivePayload {
                        when (it) {
                            is WebSocketMessageIn.RequestPayload.Message -> chatService.send(sessionInfo.userId, it)
                        }
                    }
                }
            } finally {
                chatService.closeDeviceConnection(sessionInfo.deviceId)
            }
        }

        authenticate {
            val userService = get<UserService>()
            post("getActiveDevices") { request: GetActiveDevicesRequest ->
                val devices = userService.getDevices(request.users)
                call.respond(GetActiveDevicesResponse(devices))
            }

            val conversationService = get<ConversationService>()
            post("createConversation") { request: CreateConversationRequest ->
                val authInfo = getAuthenticatedDeviceInfo()
                val id = conversationService.createConversation(authInfo.deviceId, request)
                call.respond(CreateConversationResponse(id))
            }

            get("getConversationInfo/{conversationId}") {
                val conversationId =
                    call.parameters["conversationId"] ?: throw BadRequestException("id parameter required")
                val authInfo = getAuthenticatedDeviceInfo()

                conversationService.getConversationInfoForUser(authInfo.userId, authInfo.deviceId, conversationId)
                    ?.let {
                        call.respond(
                            GetConversationInfoResponse(
                                id = it.id,
                                symmetricKey = it.symmetricKey,
                                participants = it.participants.map { user -> user.toDto() }
                            )
                        )
                    } ?: throw NotFoundException("Conversation with id = $conversationId not found")
            }
        }
    }
}

private suspend fun WebSocketServerSession.authenticate(tokenService: TokenService): SessionInfo {
    val token = withTimeout(5000L) { receiveDeserialized<String>() }
    return tokenService.verifyAccessToken(token) ?: run {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Authentication error"))
        throw BadRequestException("Authentication error")
    }
}

suspend fun WebSocketServerSession.receivePayload(handler: suspend (WebSocketMessageIn.RequestPayload) -> Unit) {
    val message = receiveDeserialized<WebSocketMessageIn.Request>()
    handler(message.payload)

    sendSerialized(WebSocketMessageOut.Ok)
}

suspend fun WebSocketServerSession.sendPayload(payload: WebSocketMessageOut.ContentPayload) {
    sendSerialized(WebSocketMessageOut.Content(payload))
    receiveDeserialized<WebSocketMessageIn.Ok>()
}

@Serializable
data class GetActiveDevicesRequest(val users: List<String>)

@Serializable
data class GetActiveDevicesResponse(val devices: List<Device>)

@Serializable
data class GetConversationInfoResponse(
    val id: String,
    val symmetricKey: String,
    val participants: List<UserDto>
)

@Serializable
data class CreateConversationRequest(
    val members: List<ConversationMember>
) {

    @Serializable
    data class ConversationMember(val deviceId: String, val conversationEncryptedKey: String)
}

@Serializable
data class CreateConversationResponse(
    val conversationId: String
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed class WebSocketMessageIn {

    @Serializable
    @SerialName("request")
    data class Request(val payload: RequestPayload) : WebSocketMessageIn()

    @JsonClassDiscriminator("type")
    @Serializable
    sealed class RequestPayload {

        @Serializable
        @SerialName("message")
        data class Message(val message: String, val conversationId: String) : RequestPayload()
    }

    @Serializable
    @SerialName("ok")
    data object Ok : WebSocketMessageIn()
}

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed class WebSocketMessageOut {

    @Serializable
    @SerialName("content")
    data class Content(val payload: ContentPayload) : WebSocketMessageOut()

    @Serializable
    @JsonClassDiscriminator("type")
    sealed class ContentPayload {

        @Serializable
        @SerialName("message")
        data class Message(
            val message: String,
            val senderId: String,
            val conversationId: String,
            @Serializable(with = OffsetDateTimeSerializer::class)
            val sentAt: OffsetDateTime
        ) : ContentPayload()
    }

    @Serializable
    @SerialName("ok")
    data object Ok : WebSocketMessageOut()
}