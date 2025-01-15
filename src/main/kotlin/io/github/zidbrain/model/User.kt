package io.github.zidbrain.model

import io.github.zidbrain.tables.UserEntity
import io.github.zidbrain.util.idString

data class User(
    val id: String,
    val email: String,
    val displayName: String
)

fun UserEntity.toModel() =
    User(id = idString, email = email, displayName = displayName)