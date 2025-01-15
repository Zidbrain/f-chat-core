package io.github.zidbrain.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object ConversationTable : UUIDTable("conversation")

class ConversationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ConversationEntity>(ConversationTable)

    var devices by DeviceEntity via DeviceConversationTable
}