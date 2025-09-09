package io.aethibo.features.articles.data.model

import io.aethibo.features.users.data.model.UserEntity
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object ArticleEntity : Table("articles") {
    val slug = varchar("slug", 100)
    val title = varchar("title", 150)
    val description = varchar("description", 150)
    val body = varchar("body", 1000)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val author = reference("author", UserEntity)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(slug, name = "article_pk")

    init {
        index(false, author)
        index(false, createdAt)
    }
}
