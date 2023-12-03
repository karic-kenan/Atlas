package io.aethibo.core.config

import io.aethibo.features.articles.data.table.Articles
import io.aethibo.features.articles.data.table.ArticlesTags
import io.aethibo.features.articles.data.table.Favorites
import io.aethibo.features.comments.data.table.Comments
import io.aethibo.features.tags.data.table.Tags
import io.aethibo.features.users.data.table.Follows
import io.aethibo.features.users.data.table.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    init {
        // In memory / keep alive between connections/transactions
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")

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

    suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction { block() }
    }
}
