package io.aethibo.core.config

import io.aethibo.features.articles.data.table.Articles
import io.aethibo.features.articles.data.table.ArticlesTags
import io.aethibo.features.articles.data.table.Favorites
import io.aethibo.features.comments.data.table.Comments
import io.aethibo.features.tags.data.table.Tags
import io.aethibo.features.users.data.table.Follows
import io.aethibo.features.users.data.table.Users
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
            create(Users)
            create(Follows)
            create(Articles)
            create(Favorites)
            create(ArticlesTags)
            create(Comments)
            create(Tags)
        }
    }
}
