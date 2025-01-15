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

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var email by UserTable.email
    var displayName by UserTable.displayName
    var contacts by UserEntity.via(ContactTable.userId, ContactTable.contactUserId)
    val devices by DeviceEntity referrersOn DeviceTable.userId
    val passwordLogin by UserPasswordLoginEntity optionalBackReferencedOn UserPasswordLogin.id
}