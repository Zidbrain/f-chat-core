package io.github.zidbrain

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.zidbrain.plugins.*
import io.github.zidbrain.service.SecretService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.ext.get


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureKoin()
        configureFirebase()
        configureApplication()
    }.start(wait = true)
}

fun Application.configureApplication() {
    configureDatabaseMigration()
    configureStatusPages()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    configureScheduling()
}

private fun Application.configureFirebase() {
    val secretService = get<SecretService>()
    val serviceAccount = secretService.getSecretFile("serviceAccountKey.json")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    FirebaseApp.initializeApp(options)
}
