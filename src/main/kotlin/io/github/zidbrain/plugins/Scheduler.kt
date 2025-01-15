package io.github.zidbrain.plugins

import io.github.zidbrain.service.ChatService
import io.ktor.server.application.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get

fun Application.configureScheduling() {
    val chat = get<ChatService>()
    scheduler(60 * 1000L) {
        chat.cleanMessages()
    }
}

fun Application.scheduler(every: Long, block: suspend () -> Unit) {
    launch {
        while(true) {
            delay(every)
            block()
        }
    }
}