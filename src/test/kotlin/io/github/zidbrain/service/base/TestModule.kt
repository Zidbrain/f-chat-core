package io.github.zidbrain.service.base

import io.github.zidbrain.service.IdTokenParser
import io.github.zidbrain.service.SecretService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
class TestModule {

    @Single
    fun secretService(): SecretService = object : SecretService {
        override fun getSecret(secret: String): String = secret
    }

    @Single
    fun idTokenParser(): IdTokenParser = object : IdTokenParser {
        override fun parseToken(idToken: String): String =
            idToken
    }
}