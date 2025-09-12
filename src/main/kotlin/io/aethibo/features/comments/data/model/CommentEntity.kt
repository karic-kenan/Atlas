package io.aethibo.features.comments.data.model

import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.users.data.model.UserEntity
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object CommentEntity : LongIdTable("comments") {
    val body = varchar("body", 1000)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val article = reference("slug", ArticleEntity.slug)
    val author = reference("author", UserEntity)

    init {
        index(false, article)
        index(false, author)
    }
}
