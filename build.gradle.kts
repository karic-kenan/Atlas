plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
}

group = "io.aethibo"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    // Ktor Server Core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.host.common)

    // Ktor Features
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.resource)
    implementation(libs.ktor.server.http.redirect)
    implementation(libs.ktor.server.hsts)
    implementation(libs.ktor.server.cors)

    // Functional
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    // Authentication & Security
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.argon2)
    implementation(libs.jwt)

    // Documentation
    implementation(libs.ktor.swagger.ui)
    implementation(libs.ktor.swagger.openapi)

    // Database - Core
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)

    // Database - Drivers & Connection Pooling
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hikaricp)

    // Dependency Injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.annotations)
    ksp(libs.koin.compiler)
    implementation(libs.koin.logger.slf4j)

    // Utilities
    implementation(libs.slugify)

    // Logging
    implementation(libs.logback.classic)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
}
