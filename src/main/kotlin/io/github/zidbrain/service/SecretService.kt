package io.github.zidbrain.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class SecretService {

    private val secrets: Map<String, String>

    init {
        val secretsJson = File("/app/resources/secrets.json").readText()
        val tree = Json.parseToJsonElement(secretsJson)
        secrets = tree.jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    fun getSecret(secret: String) = secrets[secret]!!
}