package io.github.zidbrain.model

import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.util.idString
import kotlinx.serialization.Serializable

@Serializable
data class Device(val id: String, val userId: String, val publicKey: String)

fun DeviceEntity.toModel() = Device(
    id = idString,
    userId = user.idString,
    publicKey = publicKey
)