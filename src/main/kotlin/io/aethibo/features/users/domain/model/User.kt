package io.aethibo.features.users.domain.model

import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val email: String,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val lastLoginAt: LocalDateTime? = null,
    val failedLoginAttempts: Int = 0,
    val lockedUntil: LocalDateTime? = null
)
