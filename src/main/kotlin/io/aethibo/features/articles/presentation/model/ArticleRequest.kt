package io.aethibo.features.articles.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class FindArticlesRequest(
    val tag: String? = null,
    val author: String? = null,
    val favourite: String? = null,
    val limit: Int = 20,
    val offset: Long = 0
)

@Serializable
data class CreateArticleWrapper(
    val article: CreateArticleRequest
)

@Serializable
data class CreateArticleRequest(
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String> = listOf()
)

@Serializable
data class UpdateArticleWrapper(
    val article: UpdateArticleRequest
)

@Serializable
data class UpdateArticleRequest(
    val title: String? = null,
    val description: String? = null,
    val body: String? = null,
    val tagList: List<String>? = null
)