package io.github.zidbrain.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.util.*

object DeviceTable : UUIDTable("device") {
    val publicKey = varchar("public_key", 2048).uniqueIndex()
    val userId = reference("user_id", UserTable)
    val refreshToken = varchar("device_refresh_token", 2048)
    val lastOnline = timestampWithTimeZone("device_last_online")
}

class DeviceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DeviceEntity>(DeviceTable)

    var publicKey by DeviceTable.publicKey
    var user by UserEntity referencedOn DeviceTable.userId
    var refreshToken by DeviceTable.refreshToken
    var lastOnline by DeviceTable.lastOnline
    var conversations by ConversationEntity via DeviceConversationTable
}