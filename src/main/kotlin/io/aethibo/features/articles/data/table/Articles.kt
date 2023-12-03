package io.aethibo.features.articles.data.table

import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

internal object Articles : Table() {
    val slug: Column<String> = varchar("slug", 100)
    val title: Column<String> = varchar("title", 150)
    val description: Column<String> = varchar("description", 150)
    val body: Column<String> = varchar("body", 1000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val author: Column<Long> = long("author")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(slug, name = "articlesKey")

    fun toDomain(row: ResultRow, author: User?): Article {
        return Article(
            slug = row[slug],
            title = row[title],
            description = row[description],
            body = row[body],
            createdAt = row[createdAt],
            updatedAt = row[updatedAt],
            author = author,
        )
    }
}

internal object Favorites : Table() {
    val slug: Column<String> = varchar("slug", 100)
    val user: Column<Long> = long("user")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            slug,
            user,
            name = "favouritesKey"
        )
}

internal object ArticlesTags : Table() {
    val tag: Column<Long> = long("tag")
    val slug: Column<String> = varchar("slug", 100)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            tag,
            slug,
            name = "articlesTagsKey"
        )
}
