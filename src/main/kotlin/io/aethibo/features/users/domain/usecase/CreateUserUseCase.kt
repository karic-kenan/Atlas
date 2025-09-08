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

fun interface CreateUserUseCase : suspend (User) -> Either<UserFailure, User>

suspend fun createUser(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    cipher: Cipher,
    user: User
): Either<UserFailure, User> = either {
    catch({
        val base64Encoder = Base64.getEncoder()

        val validUser = user.takeIf {
            it.email.isNotBlank() && it.username?.isNotBlank() == true && it.password?.isNotBlank() == true
        } ?: raise(UserFailure.EmptyRequiredField("email, username, or password"))

        val encryptedPassword = String(base64Encoder.encode(cipher.encrypt(validUser.password!!)))
        val userToCreate = validUser.copy(password = encryptedPassword)

        userRepository.create(userToCreate)

        validUser.copy(token = jwtProvider.createJWT(validUser))
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("user creation", exception)
        }
        raise(failure)
    }
}
