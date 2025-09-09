package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Email
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface UpdateUserUseCase : suspend (Email, User) -> Either<UserFailure, User>

suspend fun updateUser(
    userRepository: UsersRepository,
    email: Email,
    user: User
): Either<UserFailure, User> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(email.orEmpty()))

        val updatedUser = userRepository.update(validEmail, user)
            ?: raise(UserFailure.UserUpdateFailed(validEmail))

        updatedUser.copy(password = null)
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("user update", exception)
        }
        raise(failure)
    }
}
