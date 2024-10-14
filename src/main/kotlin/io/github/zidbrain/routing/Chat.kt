package io.github.zidbrain.routing

import io.github.zidbrain.dto.*
import io.github.zidbrain.service.ChatService
import io.github.zidbrain.service.ConversationService
import io.github.zidbrain.service.TokenService
import io.github.zidbrain.service.UserService
import io.github.zidbrain.service.model.SessionInfo
import io.github.zidbrain.util.getAuthenticatedDeviceInfo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeout
import org.koin.ktor.ext.get

fun Routing.chat() {
    route("/chat") {
        val chatService = get<ChatService>()

        webSocket {
            val tokenService = get<TokenService>()
            val sessionInfo = authenticate(tokenService)

            chatService.handleChatConnection(this, sessionInfo.deviceId, sessionInfo.userId)
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
                                participants = it.participants.map { user -> user.toDto() },
                            )
                        )
                    }
                    ?: throw NotFoundException("Conversation with id = $conversationId and deviceId = ${authInfo.deviceId} not found")
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