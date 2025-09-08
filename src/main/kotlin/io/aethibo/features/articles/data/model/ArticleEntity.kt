package io.aethibo.features.articles.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

object ArticleEntity : Table() {
    val slug: Column<String> = varchar("slug", 100)
    val title: Column<String> = varchar("title", 150)
    val description: Column<String> = varchar("description", 150)
    val body: Column<String> = varchar("body", 1000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val author: Column<Long> = long("author")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(slug, name = "articleKey")
}
