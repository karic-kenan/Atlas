package io.aethibo.features.articles.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

internal object ArticleTagsEntity : Table() {
    val tag: Column<Long> = long("tag")
    val slug: Column<String> = varchar("slug", 100)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            tag,
            slug,
            name = "articlesTagsKey"
        )
}
