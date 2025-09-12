package io.aethibo.core.config

import io.ktor.server.application.*

fun setup(isCio: Boolean = true, args: Array<String>) {
    if (isCio) {
        io.ktor.server.cio.EngineMain.main(args = args)
    } else {
        io.ktor.server.netty.EngineMain.main(args = args)
    }
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureSecurity()
    configureHTTP()
    configureRouting()
}
