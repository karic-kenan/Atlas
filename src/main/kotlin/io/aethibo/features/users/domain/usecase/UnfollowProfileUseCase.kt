package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Email
import io.aethibo.core.utils.Username
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface UnfollowProfileUseCase : suspend (Email, Username) -> Either<UserFailure, Profile>

suspend fun unfollowProfile(
    userRepository: UsersRepository,
    email: Email,
    usernameToUnfollow: Username
): Either<UserFailure, Profile> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(email.orEmpty()))

        val validUsername = usernameToUnfollow.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidUsername(usernameToUnfollow))

        val unfollowedUser = userRepository.unfollow(validEmail, validUsername)

        Profile(
            id = unfollowedUser.id,
            username = unfollowedUser.username,
            bio = unfollowedUser.bio,
            image = unfollowedUser.image,
            following = false
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("unfollow user", exception)
        }
        raise(failure)
    }
}
