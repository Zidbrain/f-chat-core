repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.versions)
    alias(libs.plugins.ksp)
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
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("fchat")
        imageTag.set("1.0")
    }
}

ksp {
    arg("KOIN_CONFIG_CHECK","true")
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
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.call.logging)
    implementation(libs.ktor.auth)
    implementation(libs.ktor.auth.jwt)
    implementation(libs.ktor.netty)

    implementation(libs.h2)
    implementation(libs.logback.classic)

    // tests
    testImplementation(libs.ktor.test)
    testImplementation(libs.koin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.docker.java)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.serialiazation)
}
