package io.github.zidbrain.tables

import org.jetbrains.exposed.sql.Table

object ContactTable : Table("contact") {
    val userId = reference("user_id", UserTable.id)
    val contactUserId = reference("contact_user_id", UserTable.id)

    override val primaryKey = PrimaryKey(userId, contactUserId)
}