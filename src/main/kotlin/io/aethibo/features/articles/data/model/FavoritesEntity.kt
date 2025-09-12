package io.aethibo.features.articles.data.model

import io.aethibo.features.users.data.model.UserEntity
import org.jetbrains.exposed.v1.core.Table

object FavoritesEntity : Table("favorites") {
    val slug = reference("slug", ArticleEntity.slug)
    val user = reference("user", UserEntity)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(slug, user, name = "favorites_pk")

    init {
        index(false, user)
        index(false, slug)
    }
}
