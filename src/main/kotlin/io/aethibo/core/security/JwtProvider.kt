package io.aethibo.core.security

import io.aethibo.features.users.domain.model.User
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object JwtProvider {
    private const val validityInfo = 3_600_000 * 24 // 24 hours
    private val expiresAt: Date = Date(System.currentTimeMillis() + validityInfo)
    private const val issuer = "atlas-issuer"
    const val audience = "atlas-audience"

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
}
