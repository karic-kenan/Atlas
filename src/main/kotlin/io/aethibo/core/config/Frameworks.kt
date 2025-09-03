package io.aethibo.core.config

import io.aethibo.core.di.appModule
import io.aethibo.features.articles.data.di.articlesModule
import io.aethibo.features.comments.data.di.commentsModule
import io.aethibo.features.profiles.data.di.profilesModule
import io.aethibo.features.tags.data.di.tagsModule
import io.aethibo.features.users.data.di.usersModule
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single { config }
            },
            appModule,
            usersModule,
            profilesModule,
            articlesModule,
            commentsModule,
            tagsModule
        )
    }
}
