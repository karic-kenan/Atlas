package io.aethibo.features.articles.domain.model

import io.aethibo.features.users.domain.model.User
import java.time.LocalDateTime

data class Article(
    val slug: String? = null,
    val title: String? = null,
    val description: String? = null,
    val body: String,
    val tagList: List<String> = listOf(),
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val favorited: Boolean = false,
    val favoritesCount: Long = 0,
    val author: User? = null,
)
