package io.github.zidbrain.service

import io.github.zidbrain.dao.UserDao
import io.github.zidbrain.service.base.EndToEndTest
import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.tables.UserTable
import io.github.zidbrain.util.idString
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import io.mockk.CapturingSlot
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import org.koin.test.mock.declareMock
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest : EndToEndTest() {
    private lateinit var tokenService: TokenService
    private val service: AuthService by inject()
    private val userDao: UserDao by inject()

    override fun setupMocks() {
        tokenService = declareMock()
    }

    @Suppress("SameParameterValue")
    private fun setupUserWithDevice(email: String, deviceId: UUID, devicePublicKey: String, refreshToken: String) =
        withDatabase {
            val id = UUID.randomUUID()
            val user = UserEntity.new(id) {
                this.email = email
                this.displayName = email.substringBefore('@')
            }
            DeviceEntity.new(deviceId) {
                this.publicKey = devicePublicKey
                this.user = user
                this.refreshToken = refreshToken
                this.lastOnline = OffsetDateTime.now()
            }
        }

    @Test
    fun `creates new user and issues access token`() {
        val email = "email"
        val deviceId = CapturingSlot<String>()
        val devicePublicKey = "devicePublicKey"
        val refreshToken = "refreshToken"

        every { tokenService.issueRefreshToken(any(), capture(deviceId)) } returns refreshToken

        val actual = service.getGoogleSSORefreshToken(email, devicePublicKey)
        assertEquals(refreshToken, actual.refreshToken)

        withDatabase {
            val user = UserEntity.find { UserTable.email eq email }.firstOrNull()
            assertNotNull(user)
            val device = DeviceEntity[deviceId.captured.toUUID()]
            assertEquals(refreshToken, device.refreshToken)
            assertEquals(device.user, user)
            assertEquals(device.publicKey, devicePublicKey)
        }
    }

    @Test
    fun `retrieves existing refresh token`() {
        val email = "email"
        val devicePublicKey = "devicePublicKey"
        val deviceId = UUID.randomUUID()
        val refreshToken = "refreshToken"

        every { tokenService.issueRefreshToken(any(), eq(devicePublicKey)) } returns refreshToken
        every { tokenService.verifyRefreshToken(eq(refreshToken), any(), eq(deviceId.toString())) } returns true

        setupUserWithDevice(email, deviceId, devicePublicKey, refreshToken)

        val actual = service.getGoogleSSORefreshToken(email, devicePublicKey)
        assertEquals(refreshToken, actual.refreshToken)
    }

    @Test
    fun `issues new refresh token when the last one expired`() {
        val email = "email"
        val deviceId = UUID.randomUUID()
        val devicePublicKey = "publicKey"
        val refreshToken = "refreshToken"
        val newRefreshToken = "newRefreshToken"

        every { tokenService.issueRefreshToken(any(), eq(deviceId.toString())) } returns newRefreshToken
        every { tokenService.verifyRefreshToken(eq(refreshToken), any(), eq(deviceId.toString())) } returns false

        setupUserWithDevice(email, deviceId, devicePublicKey, refreshToken)

        val actual = service.getGoogleSSORefreshToken(email, devicePublicKey)
        assertEquals(newRefreshToken, actual.refreshToken)

        withDatabase {
            val device = DeviceEntity[deviceId]
            assertEquals(newRefreshToken, device.refreshToken)
        }
    }

    // TODO: remake the following to unit
    @Test
    fun `should create user with email`() {
        val email = "email@mail.com"
        val password = "password"
        val devicePublicKey = "devicePublicKey"

        withDatabase {
            every { tokenService.issueRefreshToken(any(), any()) } returns "token"
            val refreshToken = service.createUserWithPassword(email, password, devicePublicKey)

            val actual = userDao.findUserPassword(email)
            assertEquals(refreshToken.userId, actual?.user?.idString)
            assertEquals(email, actual?.user?.email)
        }
    }

    @Test
    fun `should login user with password`() {
        val email = "email@mail.com"
        val password = "password"
        val devicePublicKey = "devicePublicKey"

        withDatabase {
            every { tokenService.issueRefreshToken(any(), any()) } returns "token"
            every { tokenService.verifyRefreshToken(eq("token"), any(), any()) } returns true
            val refreshToken = service.createUserWithPassword(email, password, devicePublicKey)

            val token = service.getPasswordAuthRefreshToken(email, password, devicePublicKey)
            assertEquals(refreshToken.userId, token.userId)
            assertEquals("token", token.refreshToken)
        }
    }

    @Test
    fun `should not login user with wrong password`() {
        val email = "email@mail.com"
        val password = "password"
        val devicePublicKey = "devicePublicKey"

        withDatabase {
            every { tokenService.issueRefreshToken(any(), any()) } returns "token"
            service.createUserWithPassword(email, password, devicePublicKey)
        }

        assertThrows<NotFoundException> {
            service.getPasswordAuthRefreshToken(email, "wrongPassword", devicePublicKey)
        }
    }
}