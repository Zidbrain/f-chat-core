package io.github.zidbrain.service

import io.github.zidbrain.tables.DeviceTable
import io.github.zidbrain.tables.UserTable
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.*

abstract class DatabaseTest {

    @JvmField
    @Rule
    val databaseTestRule = DatabaseTestRule()

    fun withDatabase(block: Transaction.() -> Unit) = transaction(databaseTestRule.database) {
        block()
    }
}

class DatabaseTestRule : TestRule {
    lateinit var database: Database

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val connection = Database.connect(JdbcDataSource().apply {
                setURL("jdbc:h2:mem:${UUID.randomUUID()};MODE=POSTGRESQL;DB_CLOSE_DELAY=-1")
            })

            transaction(connection) {
                SchemaUtils.createMissingTablesAndColumns(UserTable, DeviceTable)
            }

            database = connection

            base.evaluate()
        }
    }
}