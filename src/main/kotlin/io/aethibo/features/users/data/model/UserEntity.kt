package io.aethibo.features.users.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

internal object UserEntity : LongIdTable("users") {
    // Basic user info
    val email: Column<String> = varchar("email", 200).uniqueIndex()
    val username: Column<String?> = varchar("username", 100).nullable().uniqueIndex()
    val password: Column<String?> = varchar("password", 150).nullable()
    val bio: Column<String?> = varchar("bio", 1000).nullable()
    val image: Column<String?> = varchar("image", 255).nullable()

    // Security / token fields
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val emailVerified: Column<Boolean> = bool("email_verified").default(false)
    val failedLoginAttempts: Column<Int> = integer("failed_login_attempts").default(0)
    val lockedUntil: Column<LocalDateTime?> = datetime("locked_until").nullable()
    val lastLoginAt: Column<LocalDateTime?> = datetime("last_login_at").nullable()

    val token: Column<String?> = varchar("token", 512).nullable()
    val refreshToken: Column<String?> = varchar("refresh_token", 512).nullable()

    // Audit timestamps
    val createdAt: Column<LocalDateTime> = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt: Column<LocalDateTime> = datetime("updated_at").clientDefault { LocalDateTime.now() }
}
