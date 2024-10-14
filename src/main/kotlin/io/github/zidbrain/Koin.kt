package io.github.zidbrain

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.zidbrain.service.SecretService
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.koin
import org.koin.logger.slf4jLogger
import javax.sql.DataSource

fun Application.configureKoin(additionalConfiguration: KoinApplication.() -> Unit = {}) {
    koin {
        slf4jLogger()
        modules(AppModule().module)
        additionalConfiguration()
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

@Module
class DatabaseModule {
    @Factory
    fun dataSource(): DataSource = connect("postgres", "")

    @Factory
    fun database(dataSource: DataSource): Database = Database.connect(dataSource)
}

@Module(includes = [DatabaseModule::class])
@ComponentScan
class AppModule {

    @Single
    fun googleIdTokenVerifier(secretService: SecretService): GoogleIdTokenVerifier =
        GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), GsonFactory()
        )
            .setAudience(listOf(secretService.getSecret("googleAppId")))
            .build()
}