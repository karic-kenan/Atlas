package io.aethibo.features.articles.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleResponseDto(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: String, // ISO string for API
    val updatedAt: String, // ISO string for API
    val favorited: Boolean,
    val favoritesCount: Long,
    val author: UserResponseDto
)

@Serializable
data class ArticlesResponseDto(
    val articles: List<ArticleResponseDto>,
    val articlesCount: Int
)

@Serializable
data class UserResponseDto(
    val username: String,
    val email: String,
    val bio: String?,
    val image: String?
)
