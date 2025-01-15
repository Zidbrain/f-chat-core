@file:OptIn(ExperimentalSerializationApi::class)

package io.github.zidbrain.dto

import io.github.zidbrain.model.Device
import io.github.zidbrain.routing.UserDto
import io.github.zidbrain.util.OffsetDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.time.OffsetDateTime

@Serializable
data class GetActiveDevicesRequest(val users: List<String>)

@Serializable
data class GetActiveDevicesResponse(val devices: List<Device>)

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

@Serializable
data class GetConversationInfoResponse(
    val id: String,
    val symmetricKey: String,
    val participants: List<UserDto>
)

@Serializable
data class ChatSocketMessageIn(
    val socketMessageId: String,
    val content: ChatSocketMessageInContent
)

@JsonClassDiscriminator("type")
@Serializable
sealed class ChatSocketMessageInContent {

    @JsonClassDiscriminator("type")
    @SerialName("payload")
    @Serializable
    sealed class Payload : ChatSocketMessageInContent() {
        @Serializable
        @SerialName("messageRequest")
        data class CreateMessageRequest(
            val message: String,
            val conversationId: String,
        ) : Payload()
    }

    @JsonClassDiscriminator("type")
    @SerialName("control")
    @Serializable
    sealed class Control : ChatSocketMessageInContent() {
        @Serializable
        @SerialName("ok")
        data object Ok : Control()
    }
}

@Serializable
@JsonClassDiscriminator("type")
sealed class ChatSocketMessageOutContent {

    @JsonClassDiscriminator("type")
    @SerialName("payload")
    @Serializable
    sealed class Payload : ChatSocketMessageOutContent() {
        @Serializable
        @SerialName("message")
        data class Message(
            val externalId: String,
            val message: String,
            val senderId: String,
            val conversationId: String,
            @Serializable(with = OffsetDateTimeSerializer::class)
            val sentAt: OffsetDateTime,
        ) : Payload()
    }

    @JsonClassDiscriminator("type")
    @SerialName("control")
    @Serializable
    sealed class Control : ChatSocketMessageOutContent() {
        @Serializable
        @SerialName("messageCreated")
        data class MessageCreated(val messageId: String) : Control()

        @Serializable
        @SerialName("ok")
        data object Ok : Control()

        @Serializable
        @SerialName("error")
        data class Error(val description: String) : Control()
    }
}

@Serializable
data class ChatSocketMessageOut(
    val socketMessageId: String,
    val content: ChatSocketMessageOutContent
)