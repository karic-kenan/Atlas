package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.utils.Email
import io.aethibo.core.utils.getUserPermissions
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface GetUserByEmailUseCase : suspend (Email) -> Either<UserFailure, User>

suspend fun getUserByEmail(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    email: Email
): Either<UserFailure, User> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(email.orEmpty()))

        val user = userRepository.findByEmail(validEmail)
            ?: raise(UserFailure.UserNotFoundByEmail(validEmail))

        if (!user.isActive) {
            raise(UserFailure.UserInactive("User account is deactivated"))
        }

        val permissions = getUserPermissions(user)

        val tokenPair = jwtProvider.createTokenPair(user, permissions)

        user.copy(
            token = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            password = null // Never return password in response
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("get user by email", exception)
        }
        raise(failure)
    }
}
