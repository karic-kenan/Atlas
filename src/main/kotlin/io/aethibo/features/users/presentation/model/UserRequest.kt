package io.aethibo.features.users.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserWrapper(
    val user: RegisterUserRequest
)

@Serializable
data class RegisterUserRequest(
    val email: String,
    val password: String,
    val username: String
)

@Serializable
data class LoginUserWrapper(
    val user: LoginUserRequest
)

@Serializable
data class LoginUserRequest(
    val email: String,
    val password: String
)

@Serializable
data class UpdateUserWrapper(
    val user: UpdateUserRequest
)

@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null
)
