plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
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
    implementation(libs.exposed.java.time)

    // Database - Drivers & Connection Pooling
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hikaricp)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheuse)

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
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.datatest)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.rules.libraries)
}

tasks.test {
    useJUnitPlatform()
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
}

// Ktlint Configuration
ktlint {
    version.set("1.0.1")
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)

    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
}

// Detekt Configuration
detekt {
    toolVersion = "1.23.3"
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

// Kover Configuration (Code Coverage)
kover {
    reports {
        total {
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }

            verify {
                rule {
                    minBound(80) // Minimum 80% coverage
                }
                rule {
                    minBound(60, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
                }
            }

            filters {
                excludes {
                    classes("**/Application*", "**/plugins/**")
                }
            }
        }
    }
}

// Custom tasks for quality checks
tasks.register("qualityCheck") {
    group = "verification"
    description = "Run all quality checks"
    dependsOn("ktlintCheck", "detekt", "koverVerify", "test")
}

tasks.register("qualityFix") {
    group = "formatting"
    description = "Fix all auto-fixable quality issues"
    dependsOn("ktlintFormat", "detektAutoFix")
}

// Configure test task
tasks.test {
    useJUnitPlatform()
    finalizedBy("koverHtmlReport")
}

// Make check depend on quality checks
tasks.check {
    dependsOn("qualityCheck")
}

// Configure detekt dependencies
dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.3")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.3")
}
