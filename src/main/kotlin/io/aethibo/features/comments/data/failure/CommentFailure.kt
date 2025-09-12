package io.aethibo.features.comments.data.failure

import io.aethibo.core.exceptions.Failure
import io.ktor.http.*

sealed class CommentFailure : Failure.FeatureFailure() {
    // Comment not found operations
    data class CommentNotFound(val commentId: Long) : CommentFailure()
    data class CommentsNotFoundForSlug(val slug: String) : CommentFailure()

    // Comment creation/update issues
    data object CommentCreationFailed : CommentFailure()
    data object CommentUpdateFailed : CommentFailure()
    data object CommentDeletionFailed : CommentFailure()

    // Author related issues
    data class AuthorNotFound(val email: String) : CommentFailure()
    data class InvalidAuthorEmail(val email: String) : CommentFailure()

    // Article related issues
    data class ArticleNotFound(val slug: String) : CommentFailure()
    data class InvalidSlug(val slug: String) : CommentFailure()

    // Validation errors
    data object EmptyCommentBody : CommentFailure()
    data class InvalidCommentId(val commentId: Long) : CommentFailure()
    data class InvalidLimit(val identifier: Int) : CommentFailure()
    data class InvalidOffset(val identifier: Long) : CommentFailure()
    data class InvalidParameter(val identifier: String) : CommentFailure()

    // Permission errors
    data class UnauthorizedCommentDeletion(val commentId: Long, val userId: Long) : CommentFailure()

    // System/database errors
    data class DatabaseError(val operation: String, val cause: Throwable) : CommentFailure()
    data object RepositoryInitializationFailed : CommentFailure()
}

fun CommentFailure.getErrorMessage(): String = when (this) {
    is CommentFailure.CommentNotFound -> "Comment with ID $commentId not found"
    is CommentFailure.CommentsNotFoundForSlug -> "No comments found for article '$slug'"
    is CommentFailure.CommentCreationFailed -> "Failed to create comment"
    is CommentFailure.CommentUpdateFailed -> "Failed to update comment"
    is CommentFailure.CommentDeletionFailed -> "Failed to delete comment"
    is CommentFailure.AuthorNotFound -> "Author with email '$email' not found"
    is CommentFailure.InvalidAuthorEmail -> "Invalid author email: $email"
    is CommentFailure.ArticleNotFound -> "Article with slug '$slug' not found"
    is CommentFailure.InvalidSlug -> "Invalid slug format: '$slug'"
    is CommentFailure.EmptyCommentBody -> "Comment body cannot be empty"
    is CommentFailure.InvalidCommentId -> "Invalid comment ID: $commentId"
    is CommentFailure.UnauthorizedCommentDeletion -> "You are not authorized to delete this comment"
    is CommentFailure.DatabaseError -> "Database error during $operation: ${cause.message}"
    is CommentFailure.RepositoryInitializationFailed -> "Failed to initialize comment repository"
    is CommentFailure.InvalidLimit -> "Invalid limit $identifier"
    is CommentFailure.InvalidOffset -> "Invalid offset $identifier"
    is CommentFailure.InvalidParameter -> "Invalid parameter $identifier"
}

fun CommentFailure.toHttpStatus(): HttpStatusCode = when (this) {
    is CommentFailure.CommentNotFound -> HttpStatusCode.NotFound
    is CommentFailure.CommentsNotFoundForSlug -> HttpStatusCode.NotFound
    is CommentFailure.CommentCreationFailed -> HttpStatusCode.InternalServerError
    is CommentFailure.CommentUpdateFailed -> HttpStatusCode.InternalServerError
    is CommentFailure.CommentDeletionFailed -> HttpStatusCode.InternalServerError
    is CommentFailure.AuthorNotFound -> HttpStatusCode.NotFound
    is CommentFailure.InvalidAuthorEmail -> HttpStatusCode.BadRequest
    is CommentFailure.ArticleNotFound -> HttpStatusCode.NotFound
    is CommentFailure.InvalidSlug -> HttpStatusCode.BadRequest
    is CommentFailure.EmptyCommentBody -> HttpStatusCode.BadRequest
    is CommentFailure.InvalidCommentId -> HttpStatusCode.BadRequest
    is CommentFailure.UnauthorizedCommentDeletion -> HttpStatusCode.Forbidden
    is CommentFailure.DatabaseError -> HttpStatusCode.InternalServerError
    is CommentFailure.RepositoryInitializationFailed -> HttpStatusCode.InternalServerError
    is CommentFailure.InvalidLimit -> HttpStatusCode.BadRequest
    is CommentFailure.InvalidOffset -> HttpStatusCode.BadRequest
    is CommentFailure.InvalidParameter -> HttpStatusCode.BadRequest
}
