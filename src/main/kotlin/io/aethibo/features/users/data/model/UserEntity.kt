package io.aethibo.features.users.data.model

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object UserEntity : LongIdTable("users") {
    val email = varchar("email", 200).uniqueIndex()
    val username = varchar("username", 100).nullable().uniqueIndex()
    val password = varchar("password", 150).nullable()
    val bio = varchar("bio", 1000).nullable()
    val image = varchar("image", 255).nullable()

    val isActive = bool("is_active").default(true)
    val emailVerified = bool("email_verified").default(false)
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedUntil = datetime("locked_until").nullable()
    val lastLoginAt = datetime("last_login_at").nullable()

    val token = varchar("token", 512).nullable()
    val refreshToken = varchar("refresh_token", 512).nullable()

    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
}
