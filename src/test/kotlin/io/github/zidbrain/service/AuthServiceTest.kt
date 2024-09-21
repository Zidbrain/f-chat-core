package io.github.zidbrain.service

import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.tables.UserTable
import io.github.zidbrain.util.toUUID
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest : DatabaseTest() {
    private val tokenService: TokenService = mockk()
    private lateinit var service: AuthService

    @BeforeTest
    fun setup() {
        service = AuthService(databaseTestRule.database, tokenService)
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

        val actual = service.getOrCreateRefreshToken(email, devicePublicKey)
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

        val actual = service.getOrCreateRefreshToken(email, devicePublicKey)
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

        val actual = service.getOrCreateRefreshToken(email, devicePublicKey)
        assertEquals(newRefreshToken, actual.refreshToken)

        withDatabase {
            val device = DeviceEntity[deviceId]
            assertEquals(newRefreshToken, device.refreshToken)
        }
    }
}