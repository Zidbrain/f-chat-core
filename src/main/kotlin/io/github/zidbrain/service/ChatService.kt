package io.github.zidbrain.service

import io.github.zidbrain.dto.ChatSocketMessageIn
import io.github.zidbrain.dto.ChatSocketMessageInContent
import io.github.zidbrain.dto.ChatSocketMessageOut
import io.github.zidbrain.dto.ChatSocketMessageOutContent
import io.github.zidbrain.tables.ConversationEntity
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import java.time.OffsetDateTime
import java.util.*

@Single
class ChatService(private val database: Database) {

    private val sessions = mutableMapOf<String, WebSocketServerSession>()

    private val socketMessages =
        mutableMapOf<String, CancellableContinuation<ChatSocketMessageInContent.Control>>()

    private inline fun getSessionOr(deviceId: String, block: () -> Nothing): WebSocketServerSession {
        val session = sessions[deviceId]
        if (session?.isActive != true) {
            sessions.remove(deviceId)
            block()
        }

        return session
    }

    private suspend fun sendSocketMessageToDevice(
        session: WebSocketServerSession,
        message: ChatSocketMessageOut
    ): ChatSocketMessageInContent.Control {
        session.sendSerialized(message)

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                socketMessages.remove(message.socketMessageId)
            }
            socketMessages[message.socketMessageId] = continuation
        }
    }

    private suspend fun trySendTo(
        deviceIds: List<String>,
        message: ChatSocketMessageOutContent.Payload.Message
    ): Boolean {
        val jobs = deviceIds.mapNotNull {
            val session = getSessionOr(it) {
                return@mapNotNull null
            }
            session.async {
                sendSocketMessageToDevice(
                    session = session,
                    message = ChatSocketMessageOut(
                        socketMessageId = UUID.randomUUID().toString(),
                        content = message
                    )
                )
            }
        }
        return jobs.awaitAll().all { it is ChatSocketMessageInContent.Control.Ok }
    }

    private fun closeDeviceConnection(deviceId: String) {
        sessions.remove(deviceId)
    }

    fun cleanMessages() {

    }

    private fun openDeviceConnection(session: WebSocketServerSession, deviceId: String) {
        sessions[deviceId] = session
    }

    private fun ChatSocketMessageInContent.Payload.CreateMessageRequest.toOutMessage(
        senderId: String,
        externalMessageId: String
    ): ChatSocketMessageOutContent.Payload.Message =
        ChatSocketMessageOutContent.Payload.Message(
            externalId = externalMessageId,
            message = message,
            senderId = senderId,
            conversationId = conversationId,
            sentAt = OffsetDateTime.now()
        )

    private fun getDevicesForConversation(conversationId: String, senderDeviceId: String): List<String> =
        transaction(database) {
            ConversationEntity
                .findById(conversationId.toUUID())
                ?.devices
                ?.mapLazy { it.idString }
                ?.filter { it != senderDeviceId }?.toList() ?: throw NotFoundException()
        }

    private suspend fun send(
        toDevices: List<String>,
        senderId: String,
        externalMessageId: String,
        message: ChatSocketMessageInContent.Payload.CreateMessageRequest
    ) {
        trySendTo(
            deviceIds = toDevices,
            message = message.toOutMessage(senderId, externalMessageId)
        )
    }

    private suspend fun WebSocketServerSession.receiveMessage(userId: String, deviceId: String) {
        val message = receiveDeserialized<ChatSocketMessageIn>()
        when (val content = message.content) {
            is ChatSocketMessageInContent.Control.Ok -> launch {
                socketMessages[message.socketMessageId]?.let {
                    it.resumeWith(Result.success(content))
                    socketMessages.remove(message.socketMessageId)
                } ?: sendSerialized(
                    ChatSocketMessageOut(
                        socketMessageId = message.socketMessageId,
                        content = ChatSocketMessageOutContent.Control.Error(
                            description = "Can't find web socket request with given id"
                        )
                    )
                )
            }

            is ChatSocketMessageInContent.Payload.CreateMessageRequest -> {
                val devices =
                    getDevicesForConversation(conversationId = content.conversationId, senderDeviceId = deviceId)
                val externalId = UUID.randomUUID().toString()
                launch {
                    sendSerialized(
                        ChatSocketMessageOut(
                            socketMessageId = message.socketMessageId,
                            content = ChatSocketMessageOutContent.Control.MessageCreated(
                                messageId = externalId
                            )
                        )
                    )
                }
                launch {
                    send(
                        toDevices = devices,
                        senderId = userId,
                        externalMessageId = externalId,
                        message = content
                    )
                }
            }
        }
    }

    suspend fun handleChatConnection(session: DefaultWebSocketServerSession, deviceId: String, userId: String) {
        openDeviceConnection(session, deviceId)

        try {
            while (true) {
                session.receiveMessage(userId, deviceId)
            }
        } catch (ex: Exception) {
            if (ex !is ClosedReceiveChannelException && ex !is ClosedSendChannelException)
                throw ex
        } finally {
            closeDeviceConnection(deviceId)
        }
    }
}