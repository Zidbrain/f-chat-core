package io.github.zidbrain.model

import io.github.zidbrain.routing.WebSocketMessageIn
import java.time.OffsetDateTime
import java.util.*

data class Message(
    val content: ByteArray,
    val conversationId: UUID,
    val sentAt: OffsetDateTime,
    val received: Boolean,
    val read: Boolean,
    val senderDeviceId: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (!content.contentEquals(other.content)) return false
        if (conversationId != other.conversationId) return false
        if (sentAt != other.sentAt) return false
        if (received != other.received) return false
        if (senderDeviceId != other.senderDeviceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + sentAt.hashCode()
        result = 31 * result + received.hashCode()
        result = 31 * result + senderDeviceId.hashCode()
        return result
    }
}

fun WebSocketMessageIn.RequestPayload.Message.toModel(
    conversationId: UUID,
    senderDeviceId: UUID,
    received: Boolean
): Message =
    Message(message.encodeToByteArray(), conversationId, OffsetDateTime.now(), received, false, senderDeviceId)