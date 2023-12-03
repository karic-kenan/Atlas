package io.aethibo.features.users.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(val profile: Profile?)

@Serializable
data class Profile(
    val username: String? = null,
    val bio: String? = null,
    val image: String? = null,
    val following: Boolean = false,
)
