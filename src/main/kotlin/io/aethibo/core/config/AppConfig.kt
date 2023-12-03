package io.aethibo.core.config

import io.aethibo.core.di.appModule
import io.aethibo.core.exceptions.ErrorExceptionMapping
import io.aethibo.core.security.JwtProvider
import io.aethibo.features.users.data.di.usersModule
import io.aethibo.features.users.domain.controller.UsersController
import io.aethibo.features.users.presentation.users
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun setup(isCio: Boolean = true, args: Array<String>) {
    if (isCio) {
        io.ktor.server.cio.EngineMain.main(args = args)
    } else {
        io.ktor.server.netty.EngineMain.main(args = args)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.mainModule() {

    val jwtProvider: JwtProvider by inject()
    val userController: UsersController by inject()

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = true
            },
        )
    }
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "Atlas"
            version = "latest"
            description = "Atlas API provider for testing and demonstration purposes."
        }
        server {
            url = "http://localhost:7003/api/v1"
            description = "Development Server"
        }
    }
    install(Koin) {
        slf4jLogger()
        modules(appModule, usersModule)
    }
    install(Authentication) {
        jwt(name = "jwt") {
            verifier(jwtProvider.verifier)
            authSchemes("Token")
            realm = "atlas app"

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
    install(StatusPages) {
        ErrorExceptionMapping.register(this)
    }
    install(Routing) {
        users(userController)
    }
}
