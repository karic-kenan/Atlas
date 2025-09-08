package io.aethibo.features.users.data.failure

import io.aethibo.core.exceptions.Failure
import io.ktor.http.HttpStatusCode

sealed class UserFailure : Failure.FeatureFailure() {
    // User not found operations
    data class UserNotFound(val identifier: String) : UserFailure()
    data class UserNotFoundByEmail(val email: String) : UserFailure()
    data class UserNotFoundByUsername(val username: String) : UserFailure()

    // User creation/update issues
    data class UserAlreadyExists(val identifier: String) : UserFailure()
    data class UserCreationFailed(val email: String) : UserFailure()
    data class UserUpdateFailed(val email: String) : UserFailure()

    // Follow operations
    data class FollowOperationFailed(val followerEmail: String, val targetUsername: String) : UserFailure()
    data class UnfollowOperationFailed(val followerEmail: String, val targetUsername: String) : UserFailure()
    data class AlreadyFollowing(val followerEmail: String, val targetUsername: String) : UserFailure()
    data class NotFollowing(val followerEmail: String, val targetUsername: String) : UserFailure()
    data class SelfFollowAttempt(val email: String) : UserFailure()

    // Validation errors
    data class InvalidEmail(val email: String) : UserFailure()
    data class InvalidUsername(val username: String) : UserFailure()
    data class InvalidPassword(val reason: String) : UserFailure()
    data class EmptyRequiredField(val fieldName: String) : UserFailure()

    // System/database errors
    data class DatabaseError(val operation: String, val cause: Throwable) : UserFailure()
    data object RepositoryInitializationFailed : UserFailure()
}

fun UserFailure.getErrorMessage(): String = when (this) {
    is UserFailure.UserNotFound -> "User '$identifier' not found"
    is UserFailure.UserNotFoundByEmail -> "User with email '$email' not found"
    is UserFailure.UserNotFoundByUsername -> "User with username '$username' not found"
    is UserFailure.UserAlreadyExists -> "User '$identifier' already exists"
    is UserFailure.UserCreationFailed -> "Failed to create user with email '$email'"
    is UserFailure.UserUpdateFailed -> "Failed to update user with email '$email'"
    is UserFailure.FollowOperationFailed -> "Failed to follow user '$targetUsername'"
    is UserFailure.UnfollowOperationFailed -> "Failed to unfollow user '$targetUsername'"
    is UserFailure.AlreadyFollowing -> "You are already following '$targetUsername'"
    is UserFailure.NotFollowing -> "You are not following '$targetUsername'"
    is UserFailure.SelfFollowAttempt -> "You cannot follow yourself"
    is UserFailure.InvalidEmail -> "Invalid email format: '$email'"
    is UserFailure.InvalidUsername -> "Invalid username: '$username'"
    is UserFailure.InvalidPassword -> "Invalid password: $reason"
    is UserFailure.EmptyRequiredField -> "Required field '$fieldName' cannot be empty"
    is UserFailure.DatabaseError -> "Database error during $operation: ${cause.message}"
    is UserFailure.RepositoryInitializationFailed -> "Failed to initialize user repository"
}

fun UserFailure.toHttpStatus(): HttpStatusCode = when (this) {
    is UserFailure.UserNotFound -> HttpStatusCode.NotFound
    is UserFailure.UserNotFoundByEmail -> HttpStatusCode.NotFound
    is UserFailure.UserNotFoundByUsername -> HttpStatusCode.NotFound
    is UserFailure.UserAlreadyExists -> HttpStatusCode.Conflict
    is UserFailure.UserCreationFailed -> HttpStatusCode.InternalServerError
    is UserFailure.UserUpdateFailed -> HttpStatusCode.InternalServerError
    is UserFailure.FollowOperationFailed -> HttpStatusCode.InternalServerError
    is UserFailure.UnfollowOperationFailed -> HttpStatusCode.InternalServerError
    is UserFailure.AlreadyFollowing -> HttpStatusCode.Conflict
    is UserFailure.NotFollowing -> HttpStatusCode.BadRequest
    is UserFailure.SelfFollowAttempt -> HttpStatusCode.BadRequest
    is UserFailure.InvalidEmail -> HttpStatusCode.BadRequest
    is UserFailure.InvalidUsername -> HttpStatusCode.BadRequest
    is UserFailure.InvalidPassword -> HttpStatusCode.BadRequest
    is UserFailure.EmptyRequiredField -> HttpStatusCode.BadRequest
    is UserFailure.DatabaseError -> HttpStatusCode.InternalServerError
    is UserFailure.RepositoryInitializationFailed -> HttpStatusCode.InternalServerError
}
