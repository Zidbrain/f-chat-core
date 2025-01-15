package io.github.zidbrain.util

fun String.toSanitizedEmail(): String =
    split('@').mapIndexed { i, it ->
        if (i == 0)
            it.lowercase()
        else it
    }.joinToString("@")