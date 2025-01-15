package io.github.zidbrain.plugins

import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.get
import javax.sql.DataSource

fun Application.configureDatabaseMigration() {
    val dataSource = get<DataSource>()

    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .load()
        .migrate()
}