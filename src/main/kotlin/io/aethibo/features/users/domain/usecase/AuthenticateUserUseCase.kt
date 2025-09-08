package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.security.Cipher
import io.aethibo.core.security.JwtProvider
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import java.util.*

fun interface AuthenticateUserUseCase : suspend (User) -> Either<UserFailure, User>

suspend fun authenticateUser(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    cipher: Cipher,
    user: User
): Either<UserFailure, User> = either {
    catch({
        val base64Encoder = Base64.getEncoder()

        val email = user.email.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.InvalidEmail(user.email))

        val password = user.password?.takeIf { it.isNotBlank() }
            ?: raise(UserFailure.EmptyRequiredField("password"))

        val userFound = userRepository.findByEmail(email)
            ?: raise(UserFailure.UserNotFoundByEmail(email))

        val encryptedInputPassword = String(base64Encoder.encode(cipher.encrypt(password)))
        if (userFound.password != encryptedInputPassword) {
            raise(UserFailure.InvalidPassword("Email or password invalid"))
        }

        userFound.copy(token = jwtProvider.createJWT(userFound))
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("user authentication", exception)
        }
        raise(failure)
    }
}
