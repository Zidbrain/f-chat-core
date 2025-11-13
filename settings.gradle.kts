rootProject.name = "f-chat-core"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.2.1")
        }
    }
}
