package io.github.zidbrain

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.zidbrain.service.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import javax.sql.DataSource

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}

private fun connect(user: String, password: String): DataSource {
    val host = System.getenv("DATABASE_HOST")
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host/fchat"
        username = user
        this.password = password
    }

    return HikariDataSource(config)
}

private val appModule = module {
    single {
        val secretService = get<SecretService>()
        GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), GsonFactory()
        )
            .setAudience(listOf(secretService.getSecret("googleAppId")))
            .build()
    }
    factory { Database.connect(connect("postgres", "")) }
    singleOf(::TokenService)
    singleOf(::AuthService)
    singleOf(::SecretService)
    singleOf(::UserService)
    singleOf(::ConversationService)
    singleOf(::ChatService)
}