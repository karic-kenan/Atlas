package io.aethibo.features.comments.domain.model

import io.aethibo.features.users.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class CommentDTO(val comment: Comment?)

@Serializable
data class CommentsDTO(val comments: List<Comment>)

@Serializable
data class Comment(
    val id: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val body: String,
    val author: User? = null,
)
