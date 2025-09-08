package io.aethibo.features.comments.domain.model

import io.aethibo.features.users.domain.model.User

data class Comment(
    val id: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val body: String,
    val author: User? = null,
)
