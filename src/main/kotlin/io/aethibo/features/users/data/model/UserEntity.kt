package io.aethibo.features.users.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

internal object UserEntity : LongIdTable() {
    val email: Column<String> = varchar("email", 200).uniqueIndex()
    val username: Column<String> = varchar("username", 100).uniqueIndex()
    val password: Column<String> = varchar("password", 150)
    val bio: Column<String?> = varchar("bio", 1000).nullable()
    val image: Column<String?> = varchar("image", 255).nullable()
}
