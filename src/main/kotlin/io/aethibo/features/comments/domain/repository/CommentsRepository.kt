package io.aethibo.features.comments.domain.repository

import io.aethibo.features.comments.domain.model.Comment

interface CommentsRepository {
    suspend fun add(slugCommented: String, email: String, comment: Comment): Comment?
    suspend fun findBySlug(slug: String): List<Comment>
    suspend fun delete(id: Long, slug: String)
}
