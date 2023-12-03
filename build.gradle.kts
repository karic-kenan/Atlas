val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project
val hikaricp_version: String by project
val h2_version: String by project
val postgresql_version: String by project
val exposed_version: String by project
val jwt_version: String by project
val slugify_version: String by project
val swagger_version: String by project


plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.6"
}

group = "io.aethibo"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-default-headers")

    // Logback
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Content negotiation/serialization
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // Koin - Dependency injection
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // Hikari - Connection pooling
    implementation("com.zaxxer:HikariCP:$hikaricp_version")

    // H2 database (in memory)
    implementation("com.h2database:h2:$h2_version")

    // Postgres database (persistence)
    implementation("org.postgresql:postgresql:$postgresql_version")

    // Exposed - (sql library)
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")

    // JWT
    implementation("com.auth0:java-jwt:$jwt_version")

    // Auth - JWT
    implementation("io.ktor:ktor-server-auth-jwt")

    // Slugify
    implementation("com.github.slugify:slugify:$slugify_version")

    // Swagger
    implementation("io.github.smiley4:ktor-swagger-ui:$swagger_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.2.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
