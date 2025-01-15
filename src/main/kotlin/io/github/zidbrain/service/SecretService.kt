package io.github.zidbrain.service

import com.google.common.io.Resources
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single

fun interface SecretService {
    fun getSecret(secret: String): String
}

@Single
class SecretServiceImpl : SecretService {

    private val secrets: Map<String, String>

    init {
        val secretsJson = Resources.getResource("secrets.json").readText()
        val tree = Json.parseToJsonElement(secretsJson)
        secrets = tree.jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    override fun getSecret(secret: String) = secrets[secret]!!
}