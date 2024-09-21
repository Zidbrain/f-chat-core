package io.github.zidbrain.service

import io.github.zidbrain.model.ConversationForUser
import io.github.zidbrain.model.toModel
import io.github.zidbrain.routing.CreateConversationRequest
import io.github.zidbrain.tables.ConversationEntity
import io.github.zidbrain.tables.DeviceConversationTable
import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ConversationService(private val database: Database) {

    fun createConversation(requestedByDeviceId: String, request: CreateConversationRequest): String =
        transaction(database) {
            if (request.members.find { it.deviceId == requestedByDeviceId } == null)
                throw BadRequestException("Conversation initiator must be in conversation members")

            val uniqueUsers = DeviceEntity.forIds(request.members.map { it.deviceId.toUUID() }).asSequence()
                .distinctBy { it.user.id.value }
                .map { it.user }
            val uniqueUsersIds = uniqueUsers.map { it.id }

            val requestedByUser = DeviceEntity.findById(requestedByDeviceId.toUUID())!!.user
            val conversations = requestedByUser.devices.flatMap { device -> device.conversations }.distinctBy { it.id }
                .filter { conversation ->
                    val conversationUsers = conversation.devices.map { it.user.id }
                    uniqueUsersIds.all { it in conversationUsers }
                }.toList()
            if (conversations.size > 1) throw IllegalStateException()

            var conversation = conversations.singleOrNull()
            if (conversation != null)
                return@transaction conversation.idString

            conversation = ConversationEntity.new { }
            request.members.forEach { (deviceId, key) ->
                DeviceConversationTable.insert {
                    it[conversationId] = conversation.id.value
                    it[this.deviceId] = deviceId.toUUID()
                    it[symmetricKeyForDevice] = key
                }
            }

            return@transaction conversation.idString
        }

    fun getConversationInfoForUser(userId: String, userDeviceId: String, conversationId: String): ConversationForUser? =
        transaction(database) {
            ConversationEntity.findById(conversationId.toUUID())?.let { conversation ->
                if (conversation.devices.none { device -> device.user.idString == userId }) return@let null

                val symmetricKeyForUserDevice =
                    DeviceConversationTable.select(DeviceConversationTable.symmetricKeyForDevice).where {
                        (DeviceConversationTable.conversationId eq conversationId.toUUID()) and
                                (DeviceConversationTable.deviceId eq userDeviceId.toUUID())
                    }.firstOrNull()?.let { it[DeviceConversationTable.symmetricKeyForDevice] } ?: return@let null
                ConversationForUser(
                    id = conversation.idString,
                    symmetricKey = symmetricKeyForUserDevice,
                    participants = conversation.devices.map { it.user.toModel() }
                )
            }
        }
}