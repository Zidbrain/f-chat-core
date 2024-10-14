package io.github.zidbrain.service

import io.github.zidbrain.service.model.UserRefreshTokenInfo
import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.tables.DeviceTable
import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.tables.UserTable
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import java.time.OffsetDateTime
import java.util.*

@Single
class AuthService(
    private val database: Database,
    private val tokenService: TokenService
) {

    fun getOrCreateRefreshToken(email: String, devicePublicKey: String) = transaction(database) {
        val user = UserEntity.find { UserTable.email eq email }.firstOrNull() ?: run {
            val id = UUID.randomUUID()
            UserEntity.new(id) {
                this.email = email
                this.displayName = email.substringBefore('@')
            }
        }
        val device = DeviceEntity.find { DeviceTable.publicKey eq devicePublicKey }.firstOrNull()

        if (device != null && device.user != user)
            throw BadRequestException("Device registered to a different user")

        val userId = user.id.value.toString()
        val refreshToken = device?.refreshToken?.let { token ->
            if (tokenService.verifyRefreshToken(token, userId, device.idString))
                return@let token

            val newToken = tokenService.issueRefreshToken(userId, device.idString)
            device.refreshToken = newToken
            return@let newToken
        } ?: run {
            DeviceEntity.new {
                this.publicKey = devicePublicKey
                this.user = user
                this.refreshToken = tokenService.issueRefreshToken(userId, idString)
                this.lastOnline = OffsetDateTime.now()
            }.refreshToken
        }
        return@transaction UserRefreshTokenInfo(refreshToken, userId)
    }

    fun verifyDeviceInfo(userId: String, deviceId: String): Boolean = transaction(database) {
        return@transaction DeviceEntity.findById(deviceId.toUUID())?.user?.idString == userId
    }
}