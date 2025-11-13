plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.versions)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jib)
}

group = "com.github.zidbrain"
version = "0.0.1"

application {
    mainClass.set("com.github.zidbrain.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_23)
        localImageName.set("fchat")
        imageTag.set("1.0")
    }
}

jib {
    from {
        image = "eclipse-temurin:23-jre"
    }
    to {
        image = "fchat"
        tags = setOf("1.0")
    }
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    // koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    ksp(libs.koin.annotations.ksp.compiler)

    implementation(libs.google.api.client)

    // firebase
    implementation(libs.firebase.admin)

    // db
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.flyway.postgres)

    // exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.json)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    // ktor
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.websockets)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.serialization.kotlinx.protobuf)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.netty)

    implementation(libs.h2)
    implementation(libs.logback.classic)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.core)

    // tests
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.coroutines.debug)
}
