package io.aethibo.features.comments.presentation.model

import io.aethibo.features.articles.presentation.model.UserResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponseDto(
    val id: Long,
    val body: String,
    val createdAt: String,
    val updatedAt: String,
   val author: UserResponseDto
)

@Serializable
data class CommentsResponseDto(
    val comments: List<CommentResponseDto>
)
