package io.aethibo.features.users.data.table

import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow

internal object Users : LongIdTable() {
    val email: Column<String> = varchar("email", 200).uniqueIndex()
    val username: Column<String> = varchar("username", 100).uniqueIndex()
    val password: Column<String> = varchar("password", 150)
    val bio: Column<String?> = varchar("bio", 1000).nullable()
    val image: Column<String?> = varchar("image", 255).nullable()

    fun toDomain(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[email],
            username = row[username],
            password = row[password],
            bio = row[bio],
            image = row[image]
        )
    }
}
