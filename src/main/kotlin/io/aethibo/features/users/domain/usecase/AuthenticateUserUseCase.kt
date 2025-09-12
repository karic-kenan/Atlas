package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.SecureArgon2Cipher
import io.aethibo.core.utils.getUserPermissions
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface AuthenticateUserUseCase : suspend (User) -> Either<UserFailure, User>

suspend fun authenticateUser(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    user: User
): Either<UserFailure, User> = either {
    catch({
        val email = user.email.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(user.email))

        val password = user.password?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.EmptyRequiredField("password"))

        val userFound = userRepository.findByEmail(email)
            ?: raise(UserFailure.UserNotFoundByEmail(email))

        if (!userFound.isActive) {
            raise(UserFailure.UserInactive("User account is deactivated"))
        }

        if (!SecureArgon2Cipher.verifyPassword(password, userFound.password!!)) {
            raise(UserFailure.InvalidPassword("Email or password invalid"))
        }

        val permissions = getUserPermissions(userFound)
        val tokenPair = jwtProvider.createTokenPair(userFound, permissions)

        userFound.copy(
            token = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            password = null // Never return password
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("user authentication", exception)
        }
        raise(failure)
    }
}
