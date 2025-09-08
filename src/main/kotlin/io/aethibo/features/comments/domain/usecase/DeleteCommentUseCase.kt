package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.CommentId
import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.failure.CommentFailure
import io.aethibo.features.comments.data.failure.mapToFailure
import io.aethibo.features.comments.domain.repository.CommentRepository

fun interface DeleteCommentUseCase : suspend (CommentId, Slug) -> Either<CommentFailure, Unit>

suspend fun deleteComment(
    commentId: CommentId,
    slug: Slug,
    commentsRepository: CommentRepository,
): Either<CommentFailure, Unit> = either {
    catch({
        val validCommentId = commentId.takeIf { it > 0 }
            ?: raise(CommentFailure.InvalidCommentId(commentId))

        val validSlug = slug.takeIf { it.isNotBlank() }
            ?: raise(CommentFailure.InvalidSlug(slug))

        commentsRepository.delete(validCommentId, validSlug)
    }) { exception ->
        val failure = when (exception) {
            is CommentException -> exception.mapToFailure()
            else -> CommentFailure.DatabaseError("comment deletion", exception)
        }
        raise(failure)
    }
}
