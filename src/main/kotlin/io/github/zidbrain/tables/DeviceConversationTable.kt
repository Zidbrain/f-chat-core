package io.github.zidbrain.tables

import org.jetbrains.exposed.sql.Table

object DeviceConversationTable : Table("device_conversation") {

    val conversationId = reference("conversation_id", ConversationTable.id)
    val deviceId = reference("device_id", DeviceTable.id)
    val symmetricKeyForDevice = varchar("conversation_symmetric_key_for_device", 2048)

    override val primaryKey: PrimaryKey = PrimaryKey(conversationId, deviceId)
}