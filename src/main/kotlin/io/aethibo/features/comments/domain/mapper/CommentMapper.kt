package io.aethibo.features.comments.domain.mapper

import io.aethibo.features.articles.domain.mapper.toUserResponseDto
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
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString(),
    author = this.author?.toUserResponseDto()!!
)

fun CreateCommentRequest.toDomain(): Comment = Comment(
    id = null,
    body = this.body,
    createdAt = null, // DB will fill automatically
    updatedAt = null, // DB will fill automatically
    author = null // Will be set by use case
)
