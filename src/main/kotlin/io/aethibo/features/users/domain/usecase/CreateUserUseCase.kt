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

fun interface CreateUserUseCase : suspend (User) -> Either<UserFailure, User>

suspend fun createUser(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    user: User
): Either<UserFailure, User> = either {
    catch({
        val validUser = user.takeIf {
            it.email.isNotBlank() &&
                    it.username?.isNotBlank() == true &&
                    it.password?.isNotBlank() == true &&
                    it.password.length >= 12 && // Minimum 12 characters
                    it.password.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&].*\$"))
        }
            ?: raise(UserFailure.EmptyRequiredField("email, username, or strong password (min 12 chars with mix of cases, numbers, symbols)"))

        val hashedPassword = SecureArgon2Cipher.hashPassword(validUser.password!!)
        val userToCreate = validUser.copy(password = hashedPassword)

        userRepository.create(userToCreate)

        val permissions = getUserPermissions(userToCreate)
        val tokenPair = jwtProvider.createTokenPair(userToCreate, permissions)

        userToCreate.copy(
            token = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            password = null // Never return password in response
        )
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("user creation", exception)
        }
        raise(failure)
    }
}
