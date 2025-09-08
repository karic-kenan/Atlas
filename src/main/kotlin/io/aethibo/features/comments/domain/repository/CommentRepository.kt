package io.aethibo.features.comments.domain.repository

import io.aethibo.features.comments.domain.model.Comment

interface CommentRepository {
    suspend fun create(slugCommented: String, email: String, comment: Comment): Comment?
    suspend fun find(slug: String): List<Comment>
    suspend fun delete(id: Long, slug: String)
}
