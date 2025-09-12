package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Email
import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.failure.CommentFailure
import io.aethibo.features.comments.data.failure.mapToFailure
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentRepository

fun interface CreateCommentUseCase : suspend (Email, Slug, Comment) -> Either<CommentFailure, Comment>

suspend fun createComment(
    email: Email,
    slug: Slug,
    comment: Comment,
    commentsRepository: CommentRepository
): Either<CommentFailure, Comment> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(CommentFailure.InvalidAuthorEmail(email.orEmpty()))

        val validSlug = slug.takeIf { it.isNotBlank() }
            ?: raise(CommentFailure.InvalidSlug(slug))

        val validComment = comment.takeIf { it.body.isNotBlank() }
            ?: raise(CommentFailure.EmptyCommentBody)

        commentsRepository.create(validSlug, validEmail, validComment)
            ?: raise(CommentFailure.CommentCreationFailed)
    }) { exception ->
        val failure = when (exception) {
            is CommentException -> exception.mapToFailure()
            else -> CommentFailure.DatabaseError("comment creation", exception)
        }
        raise(failure)
    }
}
