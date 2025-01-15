package io.github.zidbrain.dao

import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.tables.UserPasswordLoginEntity
import io.github.zidbrain.tables.UserTable
import io.github.zidbrain.util.toSanitizedEmail
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single

@Single
class UserDao(
    private val database: Database
) {

    fun findUserByEmail(email: String): UserEntity? = transaction(database) {
        return@transaction UserEntity.find { UserTable.email eq email.toSanitizedEmail() }.firstOrNull()
    }

    fun findUserPassword(email: String): UserPasswordLoginEntity? = transaction(database) {
        val user = findUserByEmail(email)
        return@transaction user?.passwordLogin
    }

    fun createUserWithPassword(email: String, passwordHash: String, salt: String): UserPasswordLoginEntity =
        transaction(database) {
            val user = UserEntity.new {
                this.email = email.toSanitizedEmail()
                this.displayName = email.substringBefore('@')
            }
            return@transaction UserPasswordLoginEntity.new(user.id.value) {
                this.passwordHash = passwordHash
                this.passwordSalt = salt
            }
        }

    fun findOrCreateUser(email: String): UserEntity = transaction(database) {
        UserEntity.find { UserTable.email eq email.toSanitizedEmail() }.firstOrNull() ?: UserEntity.new {
            this.email = email
            this.displayName = email.substringBefore('@')
        }
    }
}