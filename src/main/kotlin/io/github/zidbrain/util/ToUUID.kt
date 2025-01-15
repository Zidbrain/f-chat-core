package io.github.zidbrain.util

import org.jetbrains.exposed.dao.Entity
import java.util.*

fun String.toUUID(): UUID = UUID.fromString(this)

val Entity<*>.idString: String
    get() = id.value.toString()