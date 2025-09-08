package io.aethibo.features.articles.data.failure

import io.aethibo.core.exceptions.Failure
import io.ktor.http.*

sealed class ArticleFailure : Failure.FeatureFailure() {
    // Article not found operations
    data class ArticleNotFound(val slug: String) : ArticleFailure()

    // Article creation/update issues
    data class ArticleAlreadyExists(val slug: String) : ArticleFailure()
    data object ArticleCreationFailed : ArticleFailure()
    data object ArticleUpdateFailed : ArticleFailure()

    // Author related issues
    data class AuthorNotFound(val authorId: Long) : ArticleFailure()
    data class InvalidAuthorEmail(val email: String) : ArticleFailure()
    data class InvalidUsername(val username: String) : ArticleFailure()

    // Tag related issues
    data class TagNotFound(val tagName: String) : ArticleFailure()
    data object TagCreationFailed : ArticleFailure()

    // Favorite operations
    data class ArticleAlreadyFavourite(val slug: String, val userId: Long) : ArticleFailure()
    data class ArticleNotFavourite(val slug: String, val userId: Long) : ArticleFailure()
    data object FavoriteOperationFailed : ArticleFailure()

    // Follow operations for feed
    data class UserNotFound(val email: String) : ArticleFailure()
    data object NoFollowedAuthors : ArticleFailure()

    // Search/filter operations
    data object EmptySearchResults : ArticleFailure()
    data class InvalidOffset(val offset: Long) : ArticleFailure()
    data class InvalidLimit(val limit: Int) : ArticleFailure()

    // System/database errors
    data class DatabaseError(val operation: String, val cause: Throwable) : ArticleFailure()
    data object RepositoryInitializationFailed : ArticleFailure()

    // Validation errors
    data class InvalidSlug(val slug: String) : ArticleFailure()
    data object EmptyTitle : ArticleFailure()
    data object EmptyDescription : ArticleFailure()
    data object EmptyBody : ArticleFailure()
}

fun ArticleFailure.getErrorMessage(): String = when (this) {
    is ArticleFailure.ArticleNotFound -> "Article with slug '$slug' not found"
    is ArticleFailure.ArticleAlreadyExists -> "Article with slug '$slug' already exists"
    is ArticleFailure.ArticleCreationFailed -> "Failed to create article"
    is ArticleFailure.ArticleUpdateFailed -> "Failed to update article"
    is ArticleFailure.AuthorNotFound -> "Author with ID $authorId not found"
    is ArticleFailure.InvalidAuthorEmail -> "Invalid author email: $email"
    is ArticleFailure.InvalidUsername -> "Invalid username: $username"
    is ArticleFailure.TagNotFound -> "Tag '$tagName' not found"
    is ArticleFailure.TagCreationFailed -> "Failed to create tag"
    is ArticleFailure.ArticleAlreadyFavourite -> "Article is already in your favorites"
    is ArticleFailure.ArticleNotFavourite -> "Article is not in your favorites"
    is ArticleFailure.FavoriteOperationFailed -> "Failed to update favorite status"
    is ArticleFailure.UserNotFound -> "User with email '$email' not found"
    is ArticleFailure.NoFollowedAuthors -> "You are not following any authors"
    is ArticleFailure.EmptySearchResults -> "No articles found matching your criteria"
    is ArticleFailure.InvalidOffset -> "Invalid offset value: $offset"
    is ArticleFailure.InvalidLimit -> "Invalid limit value: $limit (must be between 1 and 100)"
    is ArticleFailure.DatabaseError -> "Database error during $operation: ${cause.message}"
    is ArticleFailure.RepositoryInitializationFailed -> "Failed to initialize article repository"
    is ArticleFailure.InvalidSlug -> "Invalid slug format: '$slug'"
    is ArticleFailure.EmptyTitle -> "Article title cannot be empty"
    is ArticleFailure.EmptyDescription -> "Article description cannot be empty"
    is ArticleFailure.EmptyBody -> "Article body cannot be empty"
}

fun ArticleFailure.toHttpStatus(): HttpStatusCode = when (this) {
    is ArticleFailure.ArticleNotFound -> HttpStatusCode.NotFound
    is ArticleFailure.ArticleAlreadyExists -> HttpStatusCode.Conflict
    is ArticleFailure.ArticleCreationFailed -> HttpStatusCode.InternalServerError
    is ArticleFailure.ArticleUpdateFailed -> HttpStatusCode.InternalServerError
    is ArticleFailure.AuthorNotFound -> HttpStatusCode.BadRequest
    is ArticleFailure.InvalidAuthorEmail -> HttpStatusCode.BadRequest
    is ArticleFailure.InvalidUsername -> HttpStatusCode.BadRequest
    is ArticleFailure.TagNotFound -> HttpStatusCode.BadRequest
    is ArticleFailure.TagCreationFailed -> HttpStatusCode.InternalServerError
    is ArticleFailure.ArticleAlreadyFavourite -> HttpStatusCode.Conflict
    is ArticleFailure.ArticleNotFavourite -> HttpStatusCode.BadRequest
    is ArticleFailure.FavoriteOperationFailed -> HttpStatusCode.InternalServerError
    is ArticleFailure.UserNotFound -> HttpStatusCode.NotFound
    is ArticleFailure.NoFollowedAuthors -> HttpStatusCode.NotFound
    is ArticleFailure.EmptySearchResults -> HttpStatusCode.NotFound
    is ArticleFailure.InvalidOffset -> HttpStatusCode.BadRequest
    is ArticleFailure.InvalidLimit -> HttpStatusCode.BadRequest
    is ArticleFailure.DatabaseError -> HttpStatusCode.InternalServerError
    is ArticleFailure.RepositoryInitializationFailed -> HttpStatusCode.InternalServerError
    is ArticleFailure.InvalidSlug -> HttpStatusCode.BadRequest
    is ArticleFailure.EmptyTitle -> HttpStatusCode.BadRequest
    is ArticleFailure.EmptyDescription -> HttpStatusCode.BadRequest
    is ArticleFailure.EmptyBody -> HttpStatusCode.BadRequest
}
