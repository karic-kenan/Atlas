package io.aethibo.features.articles.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

internal object FavoritesEntity : Table() {
    val slug: Column<String> = varchar("slug", 100)
    val user: Column<Long> = long("user")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            slug,
            user,
            name = "favouritesKey"
        )
}
