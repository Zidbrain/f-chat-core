package io.github.zidbrain.service

import io.github.zidbrain.dto.*
import io.github.zidbrain.routing.UserDto
import io.github.zidbrain.service.base.EndToEndTest
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatEndToEndTest : EndToEndTest() {

    private suspend fun TestUser.createConversation(withUsers: List<TestUser>): CreateConversationResponse {
        val devices = testClient.post("/chat/getActiveDevices") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                GetActiveDevicesRequest(
                    users = (withUsers + this@createConversation).map { it.userId }
                )
            )
        }.body<GetActiveDevicesResponse>()

        return testClient.post("/chat/createConversation") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                CreateConversationRequest(
                    members = devices.devices.map {
                        CreateConversationRequest.ConversationMember(
                            deviceId = it.id,
                            conversationEncryptedKey = it.publicKey + "convo"
                        )
                    }
                )
            )
        }.body<CreateConversationResponse>()
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `two users can connect and send each other messages while both are online`() = testApplication {
        val user1 = createUserAndLogIn("test@abc.com", "key1")
        val user2 = createUserAndLogIn("test1@abc.com", "key2")

        val user1SocketSession = testClient.webSocketSession("/chat")
        user1SocketSession.sendSerialized(user1.accessToken)
        val user2SocketSession = testClient.webSocketSession("/chat")
        user2SocketSession.sendSerialized(user2.accessToken)

        val conversation = user1.createConversation(listOf(user2))

        coroutineScope {
            launch {
                val messageId = Uuid.random().toString()
                user1SocketSession.sendSerialized(
                    ChatSocketMessageIn(
                        socketMessageId = messageId,
                        content = ChatSocketMessageInContent.Payload.CreateMessageRequest(
                            message = "message out",
                            conversationId = conversation.conversationId
                        )
                    )
                )
                var response = user1SocketSession.receiveDeserialized<ChatSocketMessageOut>()
                assertEquals(messageId, response.socketMessageId)
                assertIs<ChatSocketMessageOutContent.Control.MessageCreated>(response.content)

                response = user1SocketSession.receiveDeserialized<ChatSocketMessageOut>()
                (response.content as? ChatSocketMessageOutContent.Payload.Message)?.let {
                    assertEquals("message out 2", it.message)
                    assertEquals(user2.userId, it.senderId)
                    assertEquals(conversation.conversationId, it.conversationId)
                    it
                } ?: throw AssertionError()

                user1SocketSession.sendSerialized(
                    ChatSocketMessageIn(
                        socketMessageId = messageId,
                        content = ChatSocketMessageInContent.Control.Ok
                    )
                )
            }
            launch {
                var response = user2SocketSession.receiveDeserialized<ChatSocketMessageOut>()
                val message = (response.content as? ChatSocketMessageOutContent.Payload.Message)?.let {
                    assertEquals("message out", it.message)
                    assertEquals(user1.userId, it.senderId)
                    assertEquals(conversation.conversationId, it.conversationId)
                    it
                } ?: throw AssertionError()

                val conversationInfo = testClient.get("/chat/getConversationInfo/${message.conversationId}") {
                    bearerAuth(user2.accessToken)
                    contentType(ContentType.Application.Json)
                }.body<GetConversationInfoResponse>()

                assertEquals(conversation.conversationId, conversationInfo.id)
                assertContentEquals(
                    listOf(
                        UserDto(
                            id = user1.userId,
                            email = "test@abc.com",
                            displayName = "test"
                        ),
                        UserDto(
                            id = user2.userId,
                            email = "test1@abc.com",
                            displayName = "test1"
                        )
                    ), conversationInfo.participants
                )
                assertEquals("key2convo", conversationInfo.symmetricKey)

                user2SocketSession.sendSerialized(
                    ChatSocketMessageIn(
                        socketMessageId = response.socketMessageId,
                        content = ChatSocketMessageInContent.Control.Ok
                    )
                )

                val messageId = Uuid.random().toString()
                user2SocketSession.sendSerialized(
                    ChatSocketMessageIn(
                        socketMessageId = messageId,
                        content = ChatSocketMessageInContent.Payload.CreateMessageRequest(
                            message = "message out 2",
                            conversationId = conversation.conversationId
                        )
                    )
                )
                response = user2SocketSession.receiveDeserialized<ChatSocketMessageOut>()
                assertEquals(messageId, response.socketMessageId)
                assertIs<ChatSocketMessageOutContent.Control.MessageCreated>(response.content)
            }
        }

        assertTrue { user1SocketSession.isActive }
        assertTrue { user2SocketSession.isActive }
    }
}