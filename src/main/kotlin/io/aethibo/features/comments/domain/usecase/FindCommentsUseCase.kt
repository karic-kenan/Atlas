package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.failure.CommentFailure
import io.aethibo.features.comments.data.failure.mapToFailure
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentRepository

fun interface FindCommentsUseCase : suspend (Slug) -> Either<CommentFailure, List<Comment>>

suspend fun findComments(
    slug: Slug,
    commentsRepository: CommentRepository
): Either<CommentFailure, List<Comment>> = either {
    catch({
        val validSlug = slug.takeIf { it.isNotBlank() }
            ?: raise(CommentFailure.InvalidSlug(slug))

        commentsRepository.find(validSlug)
    }) { exception ->
        val failure = when (exception) {
            is CommentException -> exception.mapToFailure()
            else -> CommentFailure.DatabaseError("find comments by slug", exception)
        }
        raise(failure)
    }
}
