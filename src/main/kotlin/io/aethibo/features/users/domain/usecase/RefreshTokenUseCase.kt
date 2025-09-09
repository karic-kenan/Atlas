package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.TokenBlacklistService
import io.aethibo.core.security.TokenPair
import io.aethibo.core.utils.getUserPermissions
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.data.failure.mapToFailure
import io.aethibo.features.users.domain.repository.UsersRepository
import java.util.*

fun interface RefreshTokenUseCase : suspend (String) -> Either<UserFailure, TokenPair>

suspend fun refreshToken(
    userRepository: UsersRepository,
    jwtProvider: JwtProvider,
    tokenBlacklistService: TokenBlacklistService,
    refreshToken: String
): Either<UserFailure, TokenPair> = either {
    catch({
        val decodedRefreshToken = jwtProvider.verifyRefreshToken(refreshToken)
            ?: raise(UserFailure.InvalidToken("Invalid refresh token"))

        val userId = decodedRefreshToken.subject
        val jti = decodedRefreshToken.getClaim("jti")?.asString()
            ?: raise(UserFailure.InvalidToken("Missing token ID"))

        // Check if refresh token is revoked
        if (tokenBlacklistService.isTokenRevoked(jti)) {
            raise(UserFailure.InvalidToken("Refresh token has been revoked"))
        }

        val user = userRepository.findByEmail(userId)
            ?: raise(UserFailure.UserNotFound("User not found"))

        // Check if user is still active
        if (!user.isActive) {
            raise(UserFailure.UserInactive("User account is deactivated"))
        }

        // Revoke the old refresh token
        tokenBlacklistService.revokeToken(jti, decodedRefreshToken.expiresAt)

        // Also revoke associated access token if present
        val accessJTI = decodedRefreshToken.getClaim("access_jti")?.asString()
        accessJTI?.let { tokenBlacklistService.revokeToken(it, Date()) }

        // Create new token pair
        val permissions = getUserPermissions(user)
        jwtProvider.createTokenPair(user, permissions)
    }) { exception ->
        val failure = when (exception) {
            is UserException -> exception.mapToFailure()
            else -> UserFailure.DatabaseError("token refresh", exception)
        }
        raise(failure)
    }
}
