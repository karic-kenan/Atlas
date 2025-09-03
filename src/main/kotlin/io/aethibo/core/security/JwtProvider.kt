package io.aethibo.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.interfaces.DecodedJWT
import io.aethibo.features.users.domain.model.User
import io.ktor.server.config.*
import java.util.*

class JwtProvider(config: ApplicationConfig) {
    private val issuer = config.property("jwt.domain").getString()
    val audience = config.property("jwt.audience").getString()
    val secret = config.property("jwt.secret").getString()
    val realm = config.property("jwt.realm").getString()

    val verifier: JWTVerifier = JWT
        .require(Cipher.algorithm)
        .withIssuer(issuer)
        .build()

    fun decodeJWT(token: String): DecodedJWT = JWT
        .require(Cipher.algorithm)
        .build()
        .verify(token)

    fun createJWT(user: User): String = JWT
        .create()
        .withIssuedAt(Date())
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("email", user.email)
        .withExpiresAt(expiresAt)
        .sign(Cipher.algorithm)

    private companion object {
        private const val VALIDITY_INFO = 3_600_000 * 24 // 24 hours
        private val expiresAt: Date = Date(System.currentTimeMillis() + VALIDITY_INFO)
    }
}
