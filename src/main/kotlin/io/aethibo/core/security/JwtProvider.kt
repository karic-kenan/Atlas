package io.aethibo.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.aethibo.features.users.domain.model.User
import io.ktor.server.config.*
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class JwtProvider(
    config: ApplicationConfig,
    private val securityConfig: SecurityConfiguration = SecurityConfiguration()
) {
    private val issuer = config.property("jwt.domain").getString()
    val audience = config.property("jwt.audience").getString()
    val realm = config.property("jwt.realm").getString()

    // Key pairs for different algorithms
    private val keyPairs = mutableMapOf<String, KeyPair>()

    // Algorithms
    private val accessAlgorithm: Algorithm
    private val refreshAlgorithm: Algorithm

    init {
        // Initialize key pairs and algorithms based on configuration
        // Default to ES256 since EdDSA is not yet supported in auth0/java-jwt
        if (securityConfig.useES256) {
            val accessKeyPair = SecureKeyManager.generateES256KeyPair()
            val refreshKeyPair = SecureKeyManager.generateES256KeyPair()
            keyPairs["access"] = accessKeyPair
            keyPairs["refresh"] = refreshKeyPair

            accessAlgorithm = Algorithm.ECDSA256(accessKeyPair.public as ECPublicKey,
                accessKeyPair.private as ECPrivateKey)
            refreshAlgorithm = Algorithm.ECDSA256(refreshKeyPair.public as ECPublicKey,
                refreshKeyPair.private as ECPrivateKey)
        } else {
            // Fallback to RS256 (not recommended for new projects)
            val accessKeyPair = SecureKeyManager.generateRSA256KeyPair()
            val refreshKeyPair = SecureKeyManager.generateRSA256KeyPair()
            keyPairs["access"] = accessKeyPair
            keyPairs["refresh"] = refreshKeyPair

            accessAlgorithm = Algorithm.RSA256(accessKeyPair.public as java.security.interfaces.RSAPublicKey,
                accessKeyPair.private as java.security.interfaces.RSAPrivateKey)
            refreshAlgorithm = Algorithm.RSA256(refreshKeyPair.public as java.security.interfaces.RSAPublicKey,
                refreshKeyPair.private as java.security.interfaces.RSAPrivateKey)
        }
    }

    val accessTokenVerifier: JWTVerifier = JWT
        .require(accessAlgorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaimPresence("type")
        .withClaimPresence("jti")
        .build()

    val refreshTokenVerifier: JWTVerifier = JWT
        .require(refreshAlgorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaimPresence("type")
        .withClaimPresence("jti")
        .build()

    fun createTokenPair(user: User, permissions: List<String> = emptyList()): TokenPair {
        val now = Instant.now()
        val accessTokenExpiry = now.plus(securityConfig.accessTokenMinutes, ChronoUnit.MINUTES)
        val refreshTokenExpiry = now.plus(securityConfig.refreshTokenDays, ChronoUnit.DAYS)

        val accessJTI = UUID.randomUUID().toString()
        val refreshJTI = UUID.randomUUID().toString()

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(accessTokenExpiry))
            .withNotBefore(Date.from(now)) // Token not valid before this time
            .withClaim("email", user.email)
            .withClaim("username", user.username)
            .withClaim("type", "access")
            .withClaim("jti", accessJTI)
            .withClaim("permissions", permissions)
            .withClaim("session_id", UUID.randomUUID().toString()) // For session tracking
            .sign(accessAlgorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(refreshTokenExpiry))
            .withNotBefore(Date.from(now))
            .withClaim("type", "refresh")
            .withClaim("jti", refreshJTI)
            .withClaim("access_jti", accessJTI) // Link to access token
            .sign(refreshAlgorithm)

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = securityConfig.accessTokenMinutes * 60 // seconds
        )
    }

    fun verifyAccessToken(token: String): DecodedJWT? {
        return try {
            val decoded = accessTokenVerifier.verify(token)

            // Additional security checks
            val tokenType = decoded.getClaim("type").asString()
            val notBefore = decoded.notBefore
            val now = Date()

            when {
                tokenType != "access" -> null
                notBefore?.after(now) == true -> null // Token not yet valid
                else -> decoded
            }
        } catch (e: Exception) {
            null
        }
    }

    fun verifyRefreshToken(token: String): DecodedJWT? {
        return try {
            val decoded = refreshTokenVerifier.verify(token)

            val tokenType = decoded.getClaim("type").asString()
            val notBefore = decoded.notBefore
            val now = Date()

            when {
                tokenType != "refresh" -> null
                notBefore?.after(now) == true -> null
                else -> decoded
            }
        } catch (e: Exception) {
            null
        }
    }

    fun extractClaims(decodedJWT: DecodedJWT): JwtClaims {
        val permissions = decodedJWT.getClaim("permissions")?.asList(String::class.java) ?: emptyList()

        return JwtClaims(
            userId = decodedJWT.subject,
            email = decodedJWT.getClaim("email").asString(),
            tokenType = decodedJWT.getClaim("type").asString(),
            issuedAt = decodedJWT.issuedAt,
            expiresAt = decodedJWT.expiresAt,
            jti = decodedJWT.getClaim("jti").asString(),
            permissions = permissions
        )
    }

    // Get public key for token verification by external services
    fun getPublicKey(keyType: String): PublicKey? = keyPairs[keyType]?.public
}
