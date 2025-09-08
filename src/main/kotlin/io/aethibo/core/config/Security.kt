package io.aethibo.core.config

import io.aethibo.core.security.JwtProvider
import io.aethibo.features.users.domain.usecase.GetUserByEmailUseCase
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtProvider: JwtProvider by inject()
    val getUserByEmailUseCase: GetUserByEmailUseCase by inject()

    install(Authentication) {
        jwt(name = "jwt") {
            verifier(jwtProvider.verifier)
            realm = jwtProvider.realm

            validate { credential ->
                val payload = credential.payload

                if (payload.audience.contains(jwtProvider.audience)) {
                    val claim = payload.claims["email"]?.asString()

                    claim?.let {
                        this.attributes.put(AttributeKey("email"), it)
                    }
                    println("Log :: Auth plugin :: Claim:$claim")

                    getUserByEmailUseCase(claim).fold(
                        ifLeft = { _ -> null },
                        ifRight = { user -> user }
                    )
                } else {
                    null
                }
            }
        }
    }
}
