package io.aethibo.core.config

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO

        filter { call ->
            !call.request.path().let { path ->
                path == "/health" ||
                        path == "/metrics" ||
                        path.startsWith("/actuator")
            }
        }

        format { call ->
            val status = call.response.status()?.value ?: 0
            val method = call.request.httpMethod.value
            val uri = call.request.uri
            val duration = call.processingTimeMillis()
            val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
            val remoteAddress = call.request.origin.remoteAddress

            "method=$method uri=$uri status=$status duration=${duration}ms remote_addr=$remoteAddress user_agent=\"$userAgent\""
        }
    }
}
