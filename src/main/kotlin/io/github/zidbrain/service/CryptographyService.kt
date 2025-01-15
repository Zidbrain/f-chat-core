package io.github.zidbrain.service

import io.ktor.util.*
import org.h2.security.SHA256
import org.koin.core.annotation.Single
import java.security.SecureRandom

@Single
class CryptographyService {

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun hashOf(text: String, salt: ByteArray): String =
        SHA256.getHashWithSalt(text.toByteArray(), salt).encodeBase64()

}