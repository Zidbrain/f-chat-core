package io.github.zidbrain.service

import com.google.common.io.Resources
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import java.io.FileInputStream
import java.io.InputStream

interface SecretService {
    fun getSecret(secret: String): String
    fun getSecretFile(filename: String): InputStream
}

@Single
class SecretServiceImpl : SecretService {

    private val secrets: Map<String, String>

    init {
        val secretsJson = Resources.getResource("secrets/secrets.json").readText()
        val tree = Json.parseToJsonElement(secretsJson)
        secrets = tree.jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    override fun getSecret(secret: String) = secrets[secret]!!

    override fun getSecretFile(filename: String): FileInputStream =
        FileInputStream(Resources.getResource("secrets/$filename").file)
}