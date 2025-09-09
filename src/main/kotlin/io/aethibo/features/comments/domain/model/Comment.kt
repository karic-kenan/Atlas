package io.aethibo.features.comments.domain.model

import io.aethibo.features.users.domain.model.User
import java.time.LocalDateTime

data class Comment(
    val id: Long? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val body: String,
    val author: User? = null,
)
