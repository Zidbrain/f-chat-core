package io.github.zidbrain.tables

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object DeviceTable : IdTable<String>("device") {
    override val id: Column<EntityID<String>> = varchar("public_key", 2048).entityId()
    val userId = reference("user_id", UserTable.id)
    val refreshToken = varchar("device_refresh_token", 2048)
    val lastOnline = timestampWithTimeZone("device_last_online")
}

class DeviceDao(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DeviceDao>(DeviceTable)

    var user by UserDao referencedOn DeviceTable.userId
    var refreshToken by DeviceTable.refreshToken
    var lastOnline by DeviceTable.lastOnline
}