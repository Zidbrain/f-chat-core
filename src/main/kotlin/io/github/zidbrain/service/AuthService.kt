package io.github.zidbrain.service

import io.github.zidbrain.dao.UserDao
import io.github.zidbrain.service.model.UserRefreshTokenInfo
import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.tables.DeviceTable
import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.tables.UserPasswordLoginEntity
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Single
class AuthService(
    private val database: Database,
    private val tokenService: TokenService,
    private val tokenParser: IdTokenParser,
    private val userDao: UserDao,
    private val cryptographyService: CryptographyService
) {

    fun createUserWithPassword(email: String, password: String, devicePublicKey: String): UserRefreshTokenInfo {
        if (userDao.findUserByEmail(email) != null)
            throw BadRequestException("User with email: $email already exists")

        val salt = cryptographyService.generateSalt()
        val hashedPassword = cryptographyService.hashOf(password, salt)
        val userPassword = userDao.createUserWithPassword(email, hashedPassword, salt.encodeBase64())

        return transaction(database) {
            getOrCreateRefreshToken(userPassword.user, devicePublicKey)
        }
    }

    fun getGoogleSSORefreshToken(idToken: String, devicePublicKey: String): UserRefreshTokenInfo {
        val email = tokenParser.parseToken(idToken)
        val user = userDao.findOrCreateUser(email)
        return getOrCreateRefreshToken(user, devicePublicKey)
    }

    fun getPasswordAuthRefreshToken(email: String, password: String, devicePublicKey: String): UserRefreshTokenInfo {
        verifyEmail(email)

        val userPassword = userDao.findUserPassword(email)
        if (userPassword == null || !verifyPassword(password, userPassword))
            throw NotFoundException("Incorrect email or password. Provided email: $email")
        return transaction(database) {
            getOrCreateRefreshToken(userPassword.user, devicePublicKey)
        }
    }

    private fun verifyPassword(password: String, userPassword: UserPasswordLoginEntity): Boolean =
        cryptographyService.hashOf(password, userPassword.passwordSalt.decodeBase64Bytes()) == userPassword.passwordHash

    private fun verifyEmail(email: String) {
        if (!emailRegex.matches(email))
            throw BadRequestException("Illegal email format: $email")
    }

    private fun getOrCreateRefreshToken(
        user: UserEntity,
        devicePublicKey: String,
    ): UserRefreshTokenInfo = transaction(database) {
        val device = DeviceEntity.find { DeviceTable.publicKey eq devicePublicKey }.firstOrNull()

        if (device != null && device.user.id != user.id)
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

    companion object {
        private val emailRegex = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    }
}