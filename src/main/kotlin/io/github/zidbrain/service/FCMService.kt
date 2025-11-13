package io.github.zidbrain.service

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import io.github.zidbrain.dto.ChatSocketMessageOutContent
import io.github.zidbrain.tables.DeviceEntity
import io.github.zidbrain.util.toUUID
import io.ktor.server.plugins.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Single
class FCMService(private val database: Database) {

    private val json = Json.Default

    fun updateToken(deviceId: String, token: String) = transaction(database) {
        val device =
            DeviceEntity.findById(deviceId.toUUID()) ?: throw NotFoundException("Device with id $deviceId not found")
        device.fcmToken = token
    }

    suspend fun sendNotificationToDevices(deviceIds: List<String>, message: ChatSocketMessageOutContent.Payload.Message) {
        val deviceTokens = transaction(database) {
            DeviceEntity.forIds(deviceIds.map { it.toUUID() }).mapNotNull { it.fcmToken }
        }

        val message = MulticastMessage.builder()
            .putAllData(
                mapOf(
                    "content" to json.encodeToString(message)
                )
            )
            .addAllTokens(deviceTokens)
            .build()

        FirebaseMessaging.getInstance().sendEachForMulticastAsync(message).await(Dispatchers.IO)
    }
}

suspend fun <F : Any?> ApiFuture<F>.await(dispatcher: CoroutineDispatcher = Dispatchers.Default): F {
    return suspendCoroutine { cont ->
        ApiFutures.addCallback(this, object : ApiFutureCallback<F> {
            override fun onFailure(t: Throwable?) {
                cont.resumeWithException(t ?: IOException("Unknown error"))
            }

            override fun onSuccess(result: F) {
                cont.resume(result)
            }
        }, dispatcher.asExecutor())
    }
}