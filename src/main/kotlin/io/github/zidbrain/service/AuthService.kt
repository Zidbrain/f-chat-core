package io.github.zidbrain.service

import io.github.zidbrain.tables.DeviceDao
import io.github.zidbrain.tables.UserDao
import io.github.zidbrain.tables.UserTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.*

class AuthService(private val database: Database, private val tokenService: TokenService) {

    fun getOrCreateRefreshToken(email: String, devicePublicKey: String) = transaction(database) {
        val user = UserDao.find { UserTable.email eq email }.firstOrNull() ?: run {
            val id = UUID.randomUUID()
            UserDao.new(id) {
                this.email = email
                this.displayName = email.substringBefore('@')
            }
        }
        val device = DeviceDao.findById(devicePublicKey)

        if (device != null && device.user != user)
            throw BadRequestException("Device registered to a different user")

        return@transaction device?.refreshToken?.let { token ->
            token.takeIf { tokenService.verifyRefreshToken(token, user.id.value.toString(), devicePublicKey) }
        } ?: run {
            DeviceDao.new(devicePublicKey) {
                this.user = user
                this.refreshToken = tokenService.issueRefreshToken(user.id.value.toString(), devicePublicKey)
                this.lastOnline = OffsetDateTime.now()
            }.refreshToken
        }
    }
}