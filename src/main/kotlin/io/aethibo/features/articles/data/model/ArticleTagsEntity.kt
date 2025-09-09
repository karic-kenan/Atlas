package io.aethibo.features.articles.data.model

import io.aethibo.features.tags.data.model.TagEntity
import org.jetbrains.exposed.v1.core.Table

object ArticleTagsEntity : Table("article_tags") {
    val tag = reference("tag", TagEntity)
    val slug = reference("slug", ArticleEntity.slug)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(tag, slug, name = "article_tags_pk")

    init {
        index(false, slug)
        index(false, tag)
    }
}
