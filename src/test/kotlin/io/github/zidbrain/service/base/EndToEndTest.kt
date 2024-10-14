package io.github.zidbrain.service.base

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import io.github.zidbrain.configureApplication
import io.github.zidbrain.configureKoin
import io.github.zidbrain.routing.GetAccessTokenRequestDto
import io.github.zidbrain.routing.GetAccessTokenResponseDto
import io.github.zidbrain.routing.GetRefreshTokenRequestDto
import io.github.zidbrain.routing.GetRefreshTokenResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class EndToEndTest : KoinTest {
    private lateinit var dockerClient: DockerClient
    private lateinit var container: CreateContainerResponse
    lateinit var testClient: HttpClient

    @BeforeTest
    fun setup() {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://localhost:2375")
            .build()
        dockerClient = DockerClientBuilder.getInstance(config).build()
        container = dockerClient.createContainerCmd("postgres:16")
            .withName("test-db")
            .withEnv("POSTGRES_DB=fchat", "POSTGRES_PASSWORD=123")
            .withHostConfig(HostConfig.newHostConfig().withPortBindings(PortBinding.parse("5433:5432")))
            .exec()
        dockerClient.startContainerCmd(container.id).exec()
    }

    @AfterTest
    fun after() {
        try {
            dockerClient.removeContainerCmd(container.id)
                .withForce(true)
                .withRemoveVolumes(true)
                .exec()
        } catch (_: Exception) {
        }
    }

    protected fun testApplication(builder: suspend ApplicationTestBuilder.() -> Unit) =
        io.ktor.server.testing.testApplication {
            application {
                configureKoin {
                    allowOverride(true)
                    modules(TestModule().module)
                }
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
                    idToken = email,
                    devicePublicKey = devicePublicKey
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