package io.aethibo.features.users.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    val id: Long? = null,
    val email: String,
    val token: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null
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
