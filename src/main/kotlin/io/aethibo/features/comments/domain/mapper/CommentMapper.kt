package io.aethibo.features.comments.domain.mapper

import io.aethibo.features.comments.data.model.CommentEntity
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.presentation.model.CommentResponseDto
import io.aethibo.features.comments.presentation.model.CreateCommentRequest
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toCommentDomain(author: User?): Comment = Comment(
    id = this[CommentEntity.id].value,
    body = this[CommentEntity.body],
    createdAt = this[CommentEntity.createdAt],
    updatedAt = this[CommentEntity.updatedAt],
    author = author,
)

fun List<Comment>.toCommentsResponseDto(): List<CommentResponseDto> = map { it.toCommentResponseDto() }


fun Comment.toCommentResponseDto(): CommentResponseDto = CommentResponseDto(
    id = this.id ?: 0L,
    body = this.body,
    createdAt = this.createdAt.toString(), // Format as needed
    updatedAt = this.updatedAt.toString(), // Format as needed
//    author = this.author?.toAuthorDto() ?: AuthorDto("", "", "", false)
)

fun CreateCommentRequest.toDomain(): Comment = Comment(
    id = null,
    body = this.body,
    createdAt = 0L, // Will be set by repository
    updatedAt = 0L, // Will be set by repository
    author = null // Will be set by use case
)
