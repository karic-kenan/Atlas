package io.aethibo.features.comments.presentation

import io.aethibo.core.exceptions.BadRequestResponse
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentsRepository
import io.aethibo.features.comments.domain.service.CommentsService

class CommentsServiceImpl(private val commentRepository: CommentsRepository) : CommentsService {
    override suspend fun add(slug: String, email: String?, comment: Comment): Comment? {
        if (email.isNullOrBlank()) throw BadRequestResponse()
        return commentRepository.add(slug, email, comment)
    }

    override suspend fun findBySlug(slug: String): List<Comment> {
        return commentRepository.findBySlug(slug)
    }

    override suspend fun delete(id: Long, slug: String) {
        return commentRepository.delete(id, slug)
    }
}
