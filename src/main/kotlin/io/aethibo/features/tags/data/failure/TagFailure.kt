package io.aethibo.features.tags.data.failure

import io.aethibo.core.exceptions.Failure
import io.ktor.http.*

sealed class TagFailure : Failure.FeatureFailure() {
    // Tag not found operations
    data object TagsNotFound : TagFailure()
    data class TagNotFound(val name: String) : TagFailure()

    // Tag creation/update issues
    data class TagCreationFailed(val name: String) : TagFailure()
    data object TagUpdateFailed : TagFailure()
    data object TagDeletionFailed : TagFailure()

    // Validation errors
    data class InvalidTagName(val name: String) : TagFailure()
    data object EmptyTagName : TagFailure()
    data class TagAlreadyExists(val name: String) : TagFailure()

    // System/database errors
    data class DatabaseError(val operation: String, val cause: Throwable) : TagFailure()
    data object RepositoryInitializationFailed : TagFailure()
}

fun TagFailure.getErrorMessage(): String = when (this) {
    is TagFailure.TagsNotFound -> "No tags found"
    is TagFailure.TagNotFound -> "Tag '$name' not found"
    is TagFailure.TagCreationFailed -> "Failed to create tag '$name'"
    is TagFailure.TagUpdateFailed -> "Failed to update tag"
    is TagFailure.TagDeletionFailed -> "Failed to delete tag"
    is TagFailure.InvalidTagName -> "Invalid tag name format: '$name'"
    is TagFailure.EmptyTagName -> "Tag name cannot be empty"
    is TagFailure.TagAlreadyExists -> "Tag '$name' already exists"
    is TagFailure.DatabaseError -> "Database error during $operation: ${cause.message}"
    is TagFailure.RepositoryInitializationFailed -> "Failed to initialize tag repository"
}

fun TagFailure.toHttpStatus(): HttpStatusCode = when (this) {
    is TagFailure.TagsNotFound -> HttpStatusCode.NotFound
    is TagFailure.TagNotFound -> HttpStatusCode.NotFound
    is TagFailure.TagCreationFailed -> HttpStatusCode.InternalServerError
    is TagFailure.TagUpdateFailed -> HttpStatusCode.InternalServerError
    is TagFailure.TagDeletionFailed -> HttpStatusCode.InternalServerError
    is TagFailure.InvalidTagName -> HttpStatusCode.BadRequest
    is TagFailure.EmptyTagName -> HttpStatusCode.BadRequest
    is TagFailure.TagAlreadyExists -> HttpStatusCode.Conflict
    is TagFailure.DatabaseError -> HttpStatusCode.InternalServerError
    is TagFailure.RepositoryInitializationFailed -> HttpStatusCode.InternalServerError
}
