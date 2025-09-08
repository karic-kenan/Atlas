package io.aethibo.features.comments.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponseDto(
    val id: Long,
    val body: String,
    val createdAt: String,
    val updatedAt: String,
//    val author: AuthorDto // Todo: Investigate
)

@Serializable
data class CommentsResponseDto(
    val comments: List<CommentResponseDto>
)
