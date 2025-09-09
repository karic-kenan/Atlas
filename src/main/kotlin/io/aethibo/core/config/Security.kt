package io.aethibo.core.config

import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.TokenBlacklistService
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.usecase.GetUserByEmailUseCase
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.koin.ktor.ext.inject
import java.util.*

fun Application.configureSecurity() {
    val jwtProvider: JwtProvider by inject()
    val getUserByEmailUseCase: GetUserByEmailUseCase by inject()
    val tokenBlacklistService: TokenBlacklistService by inject()

    install(Authentication) {
        jwt("jwt-access") {
            verifier(jwtProvider.accessTokenVerifier)
            realm = jwtProvider.realm

            validate { credential ->
                val payload = credential.payload
                val jti = payload.getClaim("jti")?.asString()

                // Enhanced security validations
                if (jti == null || tokenBlacklistService.isTokenRevoked(jti)) {
                    return@validate null
                }

                // Validate token type and timing
                val tokenType = payload.getClaim("type")?.asString()
                val notBefore = payload.notBefore
                val now = Date()

                if (tokenType != "access" || (notBefore?.after(now) == true)) {
                    return@validate null
                }

                // Validate audience
                if (!payload.audience.contains(jwtProvider.audience)) {
                    return@validate null
                }

                val email = payload.getClaim("email")?.asString()
                val userId = payload.subject
                val permissions = payload.getClaim("permissions")?.asList(String::class.java) ?: emptyList()

                email?.let { userEmail ->
                    this.attributes.put(AttributeKey("email"), userEmail)
                    this.attributes.put(AttributeKey("userId"), userId)
                    this.attributes.put(AttributeKey("jti"), jti)
                    this.attributes.put(AttributeKey("permissions"), permissions)
                }

                // Validate user still exists and is active
                getUserByEmailUseCase(email).fold(
                    ifLeft = { null },
                    ifRight = { user ->
                        if (user.isActive) user else null
                    }
                )
            }
        }

        jwt("jwt-refresh") {
            verifier(jwtProvider.refreshTokenVerifier)
            realm = jwtProvider.realm

            validate { credential ->
                val payload = credential.payload
                val jti = payload.getClaim("jti")?.asString()

                if (jti == null || tokenBlacklistService.isTokenRevoked(jti)) {
                    return@validate null
                }

                val tokenType = payload.getClaim("type")?.asString()
                val notBefore = payload.notBefore
                val now = Date()

                if (tokenType != "refresh" || (notBefore?.after(now) == true)) {
                    return@validate null
                }

                if (!payload.audience.contains(jwtProvider.audience)) {
                    return@validate null
                }

                val userId = payload.subject
                this.attributes.put(AttributeKey("userId"), userId)
                this.attributes.put(AttributeKey("jti"), jti)

                JWTPrincipal(payload)
            }
        }
    }
}
