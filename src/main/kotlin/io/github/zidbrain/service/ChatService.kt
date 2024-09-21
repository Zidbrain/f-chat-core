package io.github.zidbrain.service

import io.github.zidbrain.model.Message
import io.github.zidbrain.model.toModel
import io.github.zidbrain.routing.WebSocketMessageIn
import io.github.zidbrain.routing.WebSocketMessageOut
import io.github.zidbrain.routing.sendPayload
import io.github.zidbrain.tables.ConversationEntity
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class ChatService(private val database: Database) {

    private val sessions = mutableMapOf<String, WebSocketServerSession>()

    private val messages = mutableListOf<Message>() // TODO: move to db

    private inline fun getSessionOr(deviceId: String, block: () -> Nothing): WebSocketServerSession {
        val session = sessions[deviceId]
        if (session?.isActive != true) {
            sessions.remove(deviceId)
            block()
        }

        return session
    }

    private fun WebSocketMessageIn.RequestPayload.Message.toOutMessage(senderId: String): WebSocketMessageOut.ContentPayload.Message =
        WebSocketMessageOut.ContentPayload.Message(
            message = message,
            senderId = senderId,
            conversationId = conversationId,
            sentAt = OffsetDateTime.now()
        )

    private fun trySendTo(
        senderId: String,
        deviceIds: List<String>,
        message: WebSocketMessageIn.RequestPayload.Message
    ) {
        deviceIds.forEach {
            val session = getSessionOr(it) {
                messages.add(message.toModel(message.conversationId.toUUID(), senderId.toUUID(), false))
                return@forEach
            }

            session.launch {
                session.sendPayload(message.toOutMessage(senderId))
                messages.add(message.toModel(message.conversationId.toUUID(), senderId.toUUID(), true))
            }
        }
    }

    fun closeDeviceConnection(deviceId: String) {
        sessions.remove(deviceId)
    }

    fun cleanMessages() {

    }

    fun openDeviceConnection(session: WebSocketServerSession, deviceId: String) {
        sessions[deviceId] = session
    }

    fun send(senderId: String, message: WebSocketMessageIn.RequestPayload.Message) {
        val devices = transaction(database) {
            ConversationEntity[message.conversationId.toUUID()].devices.mapLazy { it.idString }.toList()
        }

        trySendTo(
            deviceIds = devices,
            senderId = senderId,
            message = message
        )
    }
}