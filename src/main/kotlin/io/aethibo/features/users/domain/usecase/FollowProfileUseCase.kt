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

fun interface FollowProfileUseCase : suspend (Email, Username) -> Either<UserFailure, Profile>

suspend fun followProfile(
    userRepository: UsersRepository,
    email: Email,
    usernameToFollow: Username
): Either<UserFailure, Profile> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(email.orEmpty()))

        val validUsername = usernameToFollow.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidUsername(usernameToFollow))

        val followedUser = userRepository.follow(validEmail, validUsername)

        Profile(
            id = followedUser.id,
            username = followedUser.username,
            bio = followedUser.bio,
            image = followedUser.image,
            following = true
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("follow user", exception)
        }
        raise(failure)
    }
}
