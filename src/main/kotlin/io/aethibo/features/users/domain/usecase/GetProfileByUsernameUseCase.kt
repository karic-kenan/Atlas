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

fun interface GetProfileByUsernameUseCase : suspend (Email, Username) -> Either<UserFailure, Profile>

suspend fun getProfileByUsername(
    userRepository: UsersRepository,
    email: Email,
    username: Username
): Either<UserFailure, Profile> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(email.orEmpty()))

        val validUsername = username.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidUsername(username))

        val user = userRepository.findByUsername(validUsername)
            ?: raise(UserFailure.UserNotFoundByUsername(validUsername))

        val isFollowing = userRepository.findIsFollowUser(validEmail, user.id!!)

        Profile(
            id = user.id,
            username = user.username,
            bio = user.bio,
            image = user.image,
            following = isFollowing
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("get profile by username", exception)
        }
        raise(failure)
    }
}
