package io.github.zidbrain.service

import io.github.zidbrain.dto.ChatSocketMessageOut
import io.github.zidbrain.dto.ChatSocketMessageOutContent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class ProtobufSerializationTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `correctly serializes chat messages`() {
        val message = ChatSocketMessageOut(
            socketMessageId = "1",
            content = ChatSocketMessageOutContent.Payload.Message(
                externalId = "1",
                message = "message",
                senderId = "sender",
                conversationId = "conversationId",
                sentAt = Clock.System.now()
            )
        )

        val serialized = ProtoBuf.encodeToByteArray(message)
        val deserialized = ProtoBuf.decodeFromByteArray<ChatSocketMessageOut>(serialized)
        assertEquals(message, deserialized)
    }
}