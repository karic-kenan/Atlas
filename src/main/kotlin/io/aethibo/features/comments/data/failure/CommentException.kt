package io.aethibo.features.comments.data.failure

sealed class CommentException(message: String? = null) : Exception(message) {
    // Comment not found operations
    data class CommentNotFound(val commentId: Long) : CommentException()
    data class CommentsNotFoundForSlug(val slug: String) : CommentException()

    // Comment creation/update issues
    data object CommentCreationFailed : CommentException()
    data object CommentUpdateFailed : CommentException()
    data object CommentDeletionFailed : CommentException()

    // Author related issues
    data class AuthorNotFound(val email: String) : CommentException()
    data class InvalidAuthorEmail(val email: String) : CommentException()

    // Article related issues
    data class ArticleNotFound(val slug: String) : CommentException()
    data class InvalidSlug(val slug: String) : CommentException()

    // Validation errors
    data object EmptyCommentBody : CommentException()
    data class InvalidCommentId(val commentId: Long) : CommentException()

    // Permission errors
    data class UnauthorizedCommentDeletion(val commentId: Long, val userId: Long) : CommentException()

    // System/database errors
    data class DatabaseError(val operation: String, override val cause: Throwable) : CommentException()
    data object RepositoryInitializationFailed : CommentException()
}

fun CommentException.mapToFailure(): CommentFailure = when (this) {
    is CommentException.CommentNotFound -> CommentFailure.CommentNotFound(commentId)
    is CommentException.CommentsNotFoundForSlug -> CommentFailure.CommentsNotFoundForSlug(slug)
    is CommentException.CommentCreationFailed -> CommentFailure.CommentCreationFailed
    is CommentException.CommentUpdateFailed -> CommentFailure.CommentUpdateFailed
    is CommentException.CommentDeletionFailed -> CommentFailure.CommentDeletionFailed
    is CommentException.AuthorNotFound -> CommentFailure.AuthorNotFound(email)
    is CommentException.InvalidAuthorEmail -> CommentFailure.InvalidAuthorEmail(email)
    is CommentException.ArticleNotFound -> CommentFailure.ArticleNotFound(slug)
    is CommentException.InvalidSlug -> CommentFailure.InvalidSlug(slug)
    is CommentException.EmptyCommentBody -> CommentFailure.EmptyCommentBody
    is CommentException.InvalidCommentId -> CommentFailure.InvalidCommentId(commentId)
    is CommentException.UnauthorizedCommentDeletion -> CommentFailure.UnauthorizedCommentDeletion(commentId, userId)
    is CommentException.DatabaseError -> CommentFailure.DatabaseError(operation, cause)
    is CommentException.RepositoryInitializationFailed -> CommentFailure.RepositoryInitializationFailed
}
