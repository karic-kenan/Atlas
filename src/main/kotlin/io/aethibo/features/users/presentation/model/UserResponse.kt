package io.aethibo.features.users.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    val id: Long? = null,
    val email: String,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val lastLoginAt: String? = null,
    val failedLoginAttempts: Int = 0,
    val lockedUntil: String? = null
)

@Serializable
data class UserWrapperResponseDto(
    val user: UserResponseDto
)

@Serializable
data class ProfileResponseDto(
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null,
    val following: Boolean = false,
)

@Serializable
data class ProfileWrapperResponseDto(
    val profile: ProfileResponseDto
)
