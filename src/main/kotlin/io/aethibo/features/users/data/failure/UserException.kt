package io.aethibo.features.users.data.failure

sealed class UserException(message: String? = null) : Exception(message) {
    // User not found operations
    data class UserNotFound(val identifier: String) : UserException()
    data class UserNotFoundByEmail(val email: String) : UserException()
    data class UserNotFoundByUsername(val username: String) : UserException()

    // User creation/update issues
    data class UserAlreadyExists(val identifier: String) : UserException()
    data class UserCreationFailed(val email: String) : UserException()
    data class UserUpdateFailed(val email: String) : UserException()

    // Follow operations
    data class FollowOperationFailed(val followerEmail: String, val targetUsername: String) : UserException()
    data class UnfollowOperationFailed(val followerEmail: String, val targetUsername: String) : UserException()
    data class AlreadyFollowing(val followerEmail: String, val targetUsername: String) : UserException()
    data class NotFollowing(val followerEmail: String, val targetUsername: String) : UserException()
    data class SelfFollowAttempt(val email: String) : UserException()

    // Validation errors
    data class InvalidEmail(val email: String) : UserException()
    data class InvalidUsername(val username: String) : UserException()
    data class InvalidPassword(val reason: String) : UserException()
    data class EmptyRequiredField(val fieldName: String) : UserException()
    data class InvalidLimit(val identifier: Int) : UserException()
    data class InvalidOffset(val identifier: Long) : UserException()

    // System/database errors
    data class DatabaseError(val operation: String, override val cause: Throwable) : UserException()
    data object RepositoryInitializationFailed : UserException()
}

fun UserException.mapToFailure(): UserFailure = when (this) {
    is UserException.UserNotFound -> UserFailure.UserNotFound(identifier)
    is UserException.UserNotFoundByEmail -> UserFailure.UserNotFoundByEmail(email)
    is UserException.UserNotFoundByUsername -> UserFailure.UserNotFoundByUsername(username)
    is UserException.UserAlreadyExists -> UserFailure.UserAlreadyExists(identifier)
    is UserException.UserCreationFailed -> UserFailure.UserCreationFailed(email)
    is UserException.UserUpdateFailed -> UserFailure.UserUpdateFailed(email)
    is UserException.FollowOperationFailed -> UserFailure.FollowOperationFailed(followerEmail, targetUsername)
    is UserException.UnfollowOperationFailed -> UserFailure.UnfollowOperationFailed(followerEmail, targetUsername)
    is UserException.AlreadyFollowing -> UserFailure.AlreadyFollowing(followerEmail, targetUsername)
    is UserException.NotFollowing -> UserFailure.NotFollowing(followerEmail, targetUsername)
    is UserException.SelfFollowAttempt -> UserFailure.SelfFollowAttempt(email)
    is UserException.InvalidEmail -> UserFailure.InvalidEmail(email)
    is UserException.InvalidUsername -> UserFailure.InvalidUsername(username)
    is UserException.InvalidPassword -> UserFailure.InvalidPassword(reason)
    is UserException.EmptyRequiredField -> UserFailure.EmptyRequiredField(fieldName)
    is UserException.DatabaseError -> UserFailure.DatabaseError(operation, cause)
    is UserException.RepositoryInitializationFailed -> UserFailure.RepositoryInitializationFailed
    is UserException.InvalidLimit -> UserFailure.InvalidLimit(identifier)
    is UserException.InvalidOffset -> UserFailure.InvalidOffset(identifier)
}
