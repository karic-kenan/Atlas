package io.aethibo.features.users.domain.model

data class Profile(
    val id: Long? = null,
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null,
    val following: Boolean = false,
)
