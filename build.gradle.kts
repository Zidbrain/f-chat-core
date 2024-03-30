import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("com.google.cloud.tools.jib") version "3.4.0"
}

group = "com.github.zidbrain"
version = "0.0.1"

jib {
    from {
        image = "amazoncorretto:21"
        platforms {
            platform {
                os = "linux"
                val current = DefaultNativePlatform.getCurrentArchitecture().name
                architecture =
                    if (current.contains("aarch") || current.contains("arm")) "arm64"
                    else "amd64"
            }
        }
    }
    to {
        image = "fchat"
        tags = setOf("1.0")
    }
    container {
        jvmFlags = listOf("-Xdebug")
    }
}

application {
    mainClass.set("com.github.zidbrain.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}

dependencies {
    // koin
    implementation(platform("io.insert-koin:koin-bom:$koin_version"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-ktor")
    implementation("io.insert-koin:koin-logger-slf4j")

    // google api cleint
    implementation("com.google.api-client:google-api-client:1.32.1")

    // databases
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    //ktor
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-websockets-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")

    implementation("com.h2database:h2:2.1.214")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // tests
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
