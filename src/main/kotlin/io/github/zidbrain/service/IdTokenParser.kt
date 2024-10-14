package io.github.zidbrain.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import io.ktor.server.plugins.*
import org.koin.core.annotation.Single

fun interface IdTokenParser {
    /**
     * @return User's email from the token
     */
    fun parseToken(idToken: String): String
}

@Single
class GoogleIdTokenParser(private val verifier: GoogleIdTokenVerifier) : IdTokenParser {
    override fun parseToken(idToken: String): String {
        val token = verifier.verify(idToken)?.payload ?: throw BadRequestException("Failed to get payload from token")
        return token.email
    }
}