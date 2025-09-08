package io.aethibo.core.config

import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.articles.data.model.ArticleTagsEntity
import io.aethibo.features.articles.data.model.FavoritesEntity
import io.aethibo.features.comments.data.model.CommentEntity
import io.aethibo.features.tags.data.model.TagEntity
import io.aethibo.features.users.data.model.FollowsEntity
import io.aethibo.features.users.data.model.UserEntity
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabases() {
    val config = environment.config

    // Database connection setup
    val driver = config.property("database.driver").getString()
    val url = config.property("database.url").getString()
    val username = config.propertyOrNull("database.username")?.getString() ?: ""
    val password = config.propertyOrNull("database.password")?.getString() ?: ""

    Database.connect(url, driver, username, password)

    // Schema creation
    transaction {
        SchemaUtils.run {
            create(UserEntity)
            create(FollowsEntity)
            create(ArticleEntity)
            create(FavoritesEntity)
            create(ArticleTagsEntity)
            create(CommentEntity)
            create(TagEntity)
        }
    }
}
