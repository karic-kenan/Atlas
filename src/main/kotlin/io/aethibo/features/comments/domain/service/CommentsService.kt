package io.aethibo.features.comments.domain.service

import io.aethibo.features.comments.domain.model.Comment

interface CommentsService {
    suspend fun add(slug: String, email: String?, comment: Comment): Comment?
    suspend fun findBySlug(slug: String): List<Comment>
    suspend fun delete(id: Long, slug: String)
}
