package io.aethibo.features.articles.domain.model

import io.aethibo.features.users.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class ArticleDTO(val article: Article?) {
    fun validArticle(): Article {
        require(article != null) { "Article is invalid" }
        require(article.title?.isNotBlank() ?: true) { "Title is null" }
        require(article.description?.isNotBlank() ?: true) { "Description is null" }
        require(article.body.isNotBlank()) { "Body is empty" }

        return article
    }
}

@Serializable
data class ArticlesDTO(val articles: List<Article>, val articlesCount: Int)

@Serializable
data class Article(
    val slug: String? = null,
    val title: String? = null,
    val description: String? = null,
    val body: String,
    val tagList: List<String> = listOf(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val favorited: Boolean = false,
    val favoritesCount: Long = 0,
    val author: User? = null,
)
