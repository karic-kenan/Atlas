package io.aethibo.core.security

import java.util.*

interface TokenBlacklistService {
    suspend fun isTokenRevoked(jti: String): Boolean
    suspend fun revokeToken(jti: String, expiresAt: Date)
    suspend fun revokeAllUserTokens(userId: String)
    suspend fun cleanupExpiredTokens()
}

class RedisTokenBlacklistService : TokenBlacklistService {
    // Implementation would use Redis/Valkey for production
    private val revokedTokens = mutableMapOf<String, Date>()

    override suspend fun isTokenRevoked(jti: String): Boolean {
        cleanupExpiredTokens()
        return revokedTokens.containsKey(jti)
    }

    override suspend fun revokeToken(jti: String, expiresAt: Date) {
        revokedTokens[jti] = expiresAt
    }

    override suspend fun revokeAllUserTokens(userId: String) {
        // In production, query all tokens for user and revoke them
        // This is a simplified implementation
    }

    override suspend fun cleanupExpiredTokens() {
        val now = Date()
        revokedTokens.entries.removeIf { it.value.before(now) }
    }
}
