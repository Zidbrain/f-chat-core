package io.github.zidbrain.service

import io.github.zidbrain.model.User
import io.github.zidbrain.model.toModel
import io.github.zidbrain.tables.*
import io.github.zidbrain.util.toUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import java.util.*

@Single
class UserService(private val database: Database) {

    fun getContactsFor(userId: String): List<User> = transaction(database) {
        UserEntity.findById(UUID.fromString(userId))!!.contacts.map {
            User(
                id = it.id.value.toString(),
                email = it.email,
                displayName = it.displayName
            )
        }
    }

    fun searchUsersFor(userId: String, searchString: String, limit: Int = 10): List<User> = transaction(database) {
        UserEntity.find(
            ((UserTable.email like "${searchString}%") or (UserTable.displayName like "${searchString}%"))
                    and (UserTable.id neq userId.toUUID())
        )
            .limit(limit)
            .map {
                User(it.id.value.toString(), it.email, it.displayName)
            }
    }

    fun addContact(forUserId: String, contactUserId: String) = transaction(database) {
        ContactTable.insert {
            it[userId] = forUserId.toUUID()
            it[this.contactUserId] = contactUserId.toUUID()
        }
    }

    fun removeContacts(forUserId: String, contacts: List<String>) = transaction(database) {
        val contactsIds = contacts.map { it.toUUID() }
        ContactTable.deleteWhere {
            (userId eq forUserId.toUUID()) and (this.contactUserId inList contactsIds)
        }
    }

    fun getDevices(forUsers: List<String>) = transaction(database) {
        DeviceEntity.find { DeviceTable.userId inList forUsers.map { it.toUUID() } }.mapLazy { it.toModel() }
            .toList()
    }

    fun getUserInfo(userId: String) = transaction(database) {
        UserEntity[userId.toUUID()].toModel()
    }
}
