package io.github.zidbrain.tables

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

object UserPasswordLogin : IdTable<UUID>("user_password") {

    val passwordSalt = varchar("user_password_salt", 24)
    val passwordHash = varchar("user_password_hash", 64)

    override val id: Column<EntityID<UUID>> = uuid("user_id")
        .entityId()
        .references(UserTable.id)
}

class UserPasswordLoginEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserPasswordLoginEntity>(UserPasswordLogin)

    var passwordSalt by UserPasswordLogin.passwordSalt
    var passwordHash by UserPasswordLogin.passwordHash
    var user by UserEntity referencedOn UserPasswordLogin.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserPasswordLoginEntity

        if (passwordSalt != other.passwordSalt) return false
        if (passwordHash != other.passwordHash) return false
        if (user != other.user) return false

        return true
    }

    override fun hashCode(): Int {
        var result = passwordSalt.hashCode()
        result = 31 * result + passwordHash.hashCode()
        result = 31 * result + user.hashCode()
        return result
    }
}