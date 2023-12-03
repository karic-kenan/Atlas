package io.aethibo.features.users.presentation

import io.aethibo.core.exceptions.BadRequestResponse
import io.aethibo.core.exceptions.NotAcceptableResponse
import io.aethibo.core.exceptions.NotFoundResponse
import io.aethibo.core.exceptions.UnauthorizedResponse
import io.aethibo.core.security.Cipher
import io.aethibo.core.security.JwtProvider
import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.aethibo.features.users.domain.service.UsersService
import io.ktor.server.plugins.*
import java.util.*

class UsersServiceImpl(
    private val jwtProvider: JwtProvider,
    private val cipher: Cipher,
    private val userRepository: UsersRepository
) : UsersService {
    private val base64Encoder = Base64.getEncoder()

    override suspend fun create(user: User): User {
        userRepository.findByEmail(user.email).apply {
            require(this == null) { "Email already registered!" }
        }
        userRepository.create(user.copy(password = String(base64Encoder.encode(cipher.encrypt(user.password)))))
        return user.copy(token = generateJwtToken(user))
    }

    override suspend fun authenticate(user: User): User {
        val userFound = userRepository.findByEmail(user.email)
        if (userFound?.password == String(base64Encoder.encode(Cipher.encrypt(user.password)))) {
            return userFound.copy(token = generateJwtToken(userFound))
        }
        throw UnauthorizedResponse("Email or password invalid!")
    }

    override suspend fun getByEmail(email: String): User {
        if (email.isBlank()) throw BadRequestResponse()
        val user = userRepository.findByEmail(email)
        user ?: throw NotFoundException("User not found to get.")
        return user.copy(token = generateJwtToken(user))
    }

    override suspend fun update(email: String?, user: User): User? {
        email ?: throw NotAcceptableResponse(message = "User not found to update.")
        return userRepository.update(email, user)
    }

    override suspend fun getProfileByUsername(email: String, usernameFollowing: String?): Profile {
        if (usernameFollowing.isNullOrBlank()) throw BadRequestResponse()

        return userRepository.findByUsername(usernameFollowing).let { user ->
            user ?: throw NotFoundResponse("User not found")
            Profile(
                username = user.username,
                bio = user.bio,
                image = user.image,
                following = userRepository.findIsFollowUser(email, user.id!!)
            )
        }
    }

    override suspend fun follow(email: String, usernameToFollow: String): Profile {
        return userRepository.follow(email, usernameToFollow).let { user ->
            Profile(
                username = user.username,
                bio = user.bio,
                image = user.image,
                following = true
            )
        }
    }

    override suspend fun unfollow(email: String, usernameToUnfollow: String): Profile {
        return userRepository.unfollow(email, usernameToUnfollow).let { user ->
            Profile(
                username = user.username,
                bio = user.bio,
                image = user.image,
                following = false
            )
        }
    }

    private fun generateJwtToken(user: User): String = jwtProvider.createJWT(user)
}