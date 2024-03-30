package io.github.zidbrain.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object UserTable : UUIDTable("user") {
    val email = varchar("user_email", 50)
    val displayName = varchar("user_display_name", 25)
}

class UserDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserDao>(UserTable)

    var email by UserTable.email
    var displayName by UserTable.displayName
    var contacts by UserDao.via(ContactTable.userId, ContactTable.contactUserId)
    val devices by DeviceDao referrersOn DeviceTable.userId
}