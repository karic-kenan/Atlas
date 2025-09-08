package io.aethibo.features.articles.data.failure

sealed class ArticleException(message: String? = null) : Exception(message) {
    // Article not found operations
    data class ArticleNotFound(val slug: String) : ArticleException()

    // Article creation/update issues
    data class ArticleAlreadyExists(val slug: String) : ArticleException()
    data object ArticleCreationFailed : ArticleException()
    data object ArticleUpdateFailed : ArticleException()

    // Author related issues
    data class AuthorNotFound(val authorId: Long) : ArticleException()
    data class InvalidAuthorEmail(val email: String) : ArticleException()
    data class InvalidUsername(val username: String) : ArticleException()

    // Tag related issues
    data class TagNotFound(val tagName: String) : ArticleException()
    data object TagCreationFailed : ArticleException()

    // Favorite operations
    data class ArticleAlreadyFavorited(val slug: String, val userId: Long) : ArticleException()
    data class ArticleNotFavorited(val slug: String, val userId: Long) : ArticleException()
    data object FavoriteOperationFailed : ArticleException()

    // Follow operations for feed
    data class UserNotFound(val email: String) : ArticleException()
    data object NoFollowedAuthors : ArticleException()

    // Search/filter operations
    data object EmptySearchResults : ArticleException()
    data class InvalidOffset(val offset: Long) : ArticleException()
    data class InvalidLimit(val limit: Int) : ArticleException()

    // System/database errors
    data class DatabaseError(val operation: String, override val cause: Throwable) : ArticleException()
    data object RepositoryInitializationFailed : ArticleException()

    // Validation errors
    data class InvalidSlug(val slug: String) : ArticleException()
    data object EmptyTitle : ArticleException()
    data object EmptyDescription : ArticleException()
    data object EmptyBody : ArticleException()
}

fun ArticleException.mapToFailure(): ArticleFailure = when (this) {
    is ArticleException.ArticleNotFound -> ArticleFailure.ArticleNotFound(slug)
    is ArticleException.ArticleAlreadyExists -> ArticleFailure.ArticleAlreadyExists(slug)
    is ArticleException.ArticleCreationFailed -> ArticleFailure.ArticleCreationFailed
    is ArticleException.ArticleUpdateFailed -> ArticleFailure.ArticleUpdateFailed
    is ArticleException.AuthorNotFound -> ArticleFailure.AuthorNotFound(authorId)
    is ArticleException.InvalidAuthorEmail -> ArticleFailure.InvalidAuthorEmail(email)
    is ArticleException.InvalidUsername -> ArticleFailure.InvalidUsername(username)
    is ArticleException.TagNotFound -> ArticleFailure.TagNotFound(tagName)
    is ArticleException.TagCreationFailed -> ArticleFailure.TagCreationFailed
    is ArticleException.ArticleAlreadyFavorited -> ArticleFailure.ArticleAlreadyFavourite(slug, userId)
    is ArticleException.ArticleNotFavorited -> ArticleFailure.ArticleNotFavourite(slug, userId)
    is ArticleException.FavoriteOperationFailed -> ArticleFailure.FavoriteOperationFailed
    is ArticleException.UserNotFound -> ArticleFailure.UserNotFound(email)
    is ArticleException.NoFollowedAuthors -> ArticleFailure.NoFollowedAuthors
    is ArticleException.EmptySearchResults -> ArticleFailure.EmptySearchResults
    is ArticleException.InvalidOffset -> ArticleFailure.InvalidOffset(offset)
    is ArticleException.InvalidLimit -> ArticleFailure.InvalidLimit(limit)
    is ArticleException.DatabaseError -> ArticleFailure.DatabaseError(operation, cause)
    is ArticleException.RepositoryInitializationFailed -> ArticleFailure.RepositoryInitializationFailed
    is ArticleException.InvalidSlug -> ArticleFailure.InvalidSlug(slug)
    is ArticleException.EmptyTitle -> ArticleFailure.EmptyTitle
    is ArticleException.EmptyDescription -> ArticleFailure.EmptyDescription
    is ArticleException.EmptyBody -> ArticleFailure.EmptyBody
}
