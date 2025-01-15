package io.github.zidbrain.service.base

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.zidbrain.AppModule
import io.github.zidbrain.configureApplication
import io.github.zidbrain.routing.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.mockk.mockkClass
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource
import kotlin.test.BeforeTest

@Testcontainers
abstract class EndToEndTest : KoinTest {
    lateinit var testClient: HttpClient

    private val database: Database by inject()

    open fun setupMocks() {}

    @BeforeTest
    fun setup() {
        val dataSource = get<DataSource>()

        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    @JvmField
    @RegisterExtension
    @Order(1)
    val koinExtension = KoinTestExtension.create {
        allowOverride(true)
        modules(
            listOf(
                AppModule().module,
                TestModule().module,
                module {
                    factory {
                        val config = HikariConfig().apply {
                            jdbcUrl = postgres.jdbcUrl
                            username = "postgres"
                            password = "123"
                        }
                        HikariDataSource(config)
                    } bind DataSource::class
                }
            )
        )
        setupMocks()
    }

    @JvmField
    @RegisterExtension
    @Order(0)
    val mockProviderExtension = MockProviderExtension.create {
        mockkClass(type = it, relaxUnitFun = true)
    }

    @Container
    @JvmField
    val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
        .withDatabaseName("fchat")
        .withUsername("postgres")
        .withPassword("123")

    protected fun withDatabase(block: Transaction.() -> Unit) = transaction(database) {
        block()
    }

    protected fun testApplication(builder: suspend ApplicationTestBuilder.() -> Unit) =
        io.ktor.server.testing.testApplication {
            application {
                configureApplication()
            }
            testClient = createClient {
                install(ContentNegotiation) {
                    json()
                }
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json)
                }
            }
            startApplication()
            builder()
        }

    data class TestUser(
        val userId: String,
        val refreshToken: String,
        val accessToken: String
    )

    protected suspend fun createUserAndLogIn(email: String, devicePublicKey: String): TestUser {
        val refreshToken = testClient.post("/auth/getRefreshToken") {
            contentType(ContentType.Application.Json)
            setBody(
                GetRefreshTokenRequestDto(
                    GetRefreshTokenRequestDtoData.GoogleSSO(
                        idToken = email,
                        devicePublicKey = devicePublicKey,
                    )
                )
            )
        }.body<GetRefreshTokenResponseDto>()

        val accessToken = testClient.post("/auth/getAccessToken") {
            contentType(ContentType.Application.Json)
            setBody(
                GetAccessTokenRequestDto(refreshToken = refreshToken.refreshToken)
            )
        }.body<GetAccessTokenResponseDto>()

        return TestUser(
            userId = refreshToken.userId,
            refreshToken = refreshToken.refreshToken,
            accessToken = accessToken.accessToken
        )
    }
}