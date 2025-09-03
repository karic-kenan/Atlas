package io.aethibo.core.config

import io.aethibo.core.security.JwtProvider
import io.aethibo.features.users.domain.controller.UsersController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtProvider: JwtProvider by inject()
    val userController: UsersController by inject()

    install(Authentication) {
        jwt(name = "jwt") {
            verifier(jwtProvider.verifier)
            authSchemes("Token")
            realm = jwtProvider.realm

            validate { credential ->
                val payload = credential.payload

                if (payload.audience.contains(jwtProvider.audience)) {
                    val claim = payload.claims["email"]?.asString()

                    claim?.let {
                        this.attributes.put(AttributeKey("email"), it)
                    }

                    userController.getUserByEmail(claim)
                } else {
                    null
                }
            }
        }
    }
}
