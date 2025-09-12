package io.aethibo.core.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.articles.data.model.ArticleTagsEntity
import io.aethibo.features.articles.data.model.FavoritesEntity
import io.aethibo.features.comments.data.model.CommentEntity
import io.aethibo.features.tags.data.model.TagEntity
import io.aethibo.features.users.data.model.FollowsEntity
import io.aethibo.features.users.data.model.UserEntity
import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.get

fun Application.configureDatabases() {
    val config = environment.config.config("database")

    val hikariConfig = HikariConfig().apply {
        driverClassName = config.property("driver").getString()
        jdbcUrl = config.property("url").getString()
        username = config.propertyOrNull("username")?.getString()
        password = config.propertyOrNull("password")?.getString()

        maximumPoolSize = config.propertyOrNull("maximumPoolSize")?.getString()?.toInt() ?: 10
        minimumIdle = config.propertyOrNull("minimumIdle")?.getString()?.toInt() ?: 2
        idleTimeout = config.propertyOrNull("idleTimeout")?.getString()?.toLong() ?: 60_000
        maxLifetime = config.propertyOrNull("maxLifetime")?.getString()?.toLong() ?: (30 * 60 * 1000)
        connectionTimeout = config.propertyOrNull("connectionTimeout")?.getString()?.toLong() ?: 10_000

        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(
            UserEntity,
            FollowsEntity,
            ArticleEntity,
            FavoritesEntity,
            ArticleTagsEntity,
            CommentEntity,
            TagEntity
        )
    }

    monitor.subscribe(ApplicationStopping) {
        dataSource.close()
    }

    monitorHikari(dataSource)
}

fun Application.monitorHikari(dataSource: HikariDataSource) {
    val meterRegistry = get<MeterRegistry>()
    dataSource.metricRegistry = null
    dataSource.metricsTrackerFactory = MicrometerMetricsTrackerFactory(meterRegistry)
}
