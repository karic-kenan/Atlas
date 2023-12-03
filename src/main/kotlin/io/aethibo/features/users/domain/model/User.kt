package io.aethibo.features.users.domain.model

import io.aethibo.core.extensions.isEmailValid
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(val user: User? = null) {
    fun validRegister(): User {
        require(user != null) { "User is invalid." }
        require(user.email.isNotBlank()) { "Email is empty" }
        require(user.email.isEmailValid()) { "Email is not valid" }
        require(!user.password.isNullOrBlank()) { "Password is empty" }
        require(!user.username.isNullOrBlank()) { "Username is empty" }

        return user
    }

    fun validLogin(): User {
        require(user != null) { "User is invalid." }
        require(user.email.isNotBlank()) { "Email is empty" }
        require(user.email.isEmailValid()) { "Email is not valid" }
        require(!user.password.isNullOrBlank()) { "Password is empty" }

        return user
    }

    fun validToUpdate(): User {
        require(user != null) { "User is invalid" }
        require(user.email.isNotBlank()) { "Email is empty" }
        require(user.email.isEmailValid()) { "Email is not valid" }
        require(user.password?.isNotBlank() ?: true) { "Password must not be empty" }
        require(user.username?.isNotBlank() ?: true) { "Username must not be empty" }
        require(user.bio?.isNotBlank() ?: true) { "Bio must not be empty" }
        require(user.image?.isNotBlank() ?: true) { "Image must not be empty" }

        return user
    }
}

@Serializable
data class User(
    val id: Long? = null,
    val email: String,
    val token: String? = null,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
) : Principal
