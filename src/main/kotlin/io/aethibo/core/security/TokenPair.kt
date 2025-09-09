package io.aethibo.core.security

import java.util.*

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)

data class JwtClaims(
    val userId: String,
    val email: String,
    val tokenType: String,
    val issuedAt: Date,
    val expiresAt: Date,
    val jti: String,
    val permissions: List<String> = emptyList()
)

data class SecurityConfiguration(
    val useES256: Boolean = true,        // Prefer ES256 over RS256
    val useEdDSA: Boolean = false,       // Best security but newer
    val accessTokenMinutes: Long = 15,   // Short-lived access tokens
    val refreshTokenDays: Long = 30,     // Longer refresh tokens
    val enableJTI: Boolean = true,       // JWT ID for revocation
    val requireSecureTransport: Boolean = true
)
