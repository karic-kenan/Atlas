package io.aethibo.core.config

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    val isDevelopment = developmentMode
    install(OpenApi)

    routing {
        route("api.json") {
            openApi()
        }

        route("swagger") {
            swaggerUI("/api.json") {
                // Add configuration for this Swagger UI "instance" here.
            }
        }
    }

    install(DefaultHeaders) {
        // Will send this header with each response
        header("X-Engine", "Ktor")

        // Prevent clickjacking
        header("X-Frame-Options", "DENY")

        // Prevent MIME type sniffing
        header("X-Content-Type-Options", "nosniff")

        // XSS Protection
        header("X-XSS-Protection", "1; mode=block")

        // Force HTTPS
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")

        // Content Security Policy (strict)
        header(
            "Content-Security-Policy",
            "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"
        )

        // Additional security headers
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        header("X-Permitted-Cross-Domain-Policies", "none")
        header("Cross-Origin-Embedder-Policy", "require-corp")
        header("Cross-Origin-Opener-Policy", "same-origin")
        header("Cross-Origin-Resource-Policy", "same-origin")
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true

        // Only allow specific origins in production
        if (isDevelopment) {
            allowHost("localhost:3000")
            allowHost("127.0.0.1:3000")
        } else {
            // Configure your production domains
            allowHost("your-domain.com", schemes = listOf("https"))
        }
    }

    // Force HTTPS in production
    if (!isDevelopment) {
        install(HttpsRedirect) {
            sslPort = 443
            permanentRedirect = true
        }

        install(HSTS) {
            includeSubDomains = true
            preload = true
            maxAgeInSeconds = 31536000 // 1 year
        }
    }
}
