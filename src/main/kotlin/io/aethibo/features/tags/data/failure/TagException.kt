package io.aethibo.features.tags.data.failure

sealed class TagException(message: String? = null) : Exception(message) {
    // Tag not found operations
    data object TagsNotFound : TagException()
    data class TagNotFound(val name: String) : TagException()

    // Tag creation/update issues
    data class TagCreationFailed(val name: String) : TagException()
    data object TagUpdateFailed : TagException()
    data object TagDeletionFailed : TagException()

    // Validation errors
    data class InvalidTagName(val name: String) : TagException()
    data object EmptyTagName : TagException()
    data class TagAlreadyExists(val name: String) : TagException()

    // System/database errors
    data class DatabaseError(val operation: String, override val cause: Throwable) : TagException()
    data object RepositoryInitializationFailed : TagException()
}

fun TagException.mapToFailure(): TagFailure = when (this) {
    is TagException.TagsNotFound -> TagFailure.TagsNotFound
    is TagException.TagNotFound -> TagFailure.TagNotFound(name)
    is TagException.TagCreationFailed -> TagFailure.TagCreationFailed(name)
    is TagException.TagUpdateFailed -> TagFailure.TagUpdateFailed
    is TagException.TagDeletionFailed -> TagFailure.TagDeletionFailed
    is TagException.InvalidTagName -> TagFailure.InvalidTagName(name)
    is TagException.EmptyTagName -> TagFailure.EmptyTagName
    is TagException.TagAlreadyExists -> TagFailure.TagAlreadyExists(name)
    is TagException.DatabaseError -> TagFailure.DatabaseError(operation, cause)
    is TagException.RepositoryInitializationFailed -> TagFailure.RepositoryInitializationFailed
}
