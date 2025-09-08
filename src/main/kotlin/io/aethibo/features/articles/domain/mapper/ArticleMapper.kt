package io.aethibo.features.articles.domain.mapper

import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.presentation.model.*
import io.aethibo.features.users.domain.model.User
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toArticleDomain(author: User): Article = Article(
    slug = this[ArticleEntity.slug],
    title = this[ArticleEntity.title],
    description = this[ArticleEntity.description],
    body = this[ArticleEntity.body],
    createdAt = this[ArticleEntity.createdAt],
    updatedAt = this[ArticleEntity.updatedAt],
    author = author
)

fun CreateArticleRequest.toDomain(): Article = Article(
    slug = "", // Will be generated
    title = this.title,
    description = this.description,
    body = this.body,
    tagList = this.tagList,
    createdAt = 0L, // Will be set by service
    updatedAt = 0L, // Will be set by service
)

fun UpdateArticleRequest.toDomain(): Article = Article(
    slug = "", // Will be set by service
    title = this.title ?: "",
    description = this.description ?: "",
    body = this.body ?: "",
    tagList = this.tagList ?: listOf(),
    createdAt = 0L, // Will be preserved by service
    updatedAt = 0L, // Will be preserved by service
)

fun Article.toArticleResponseDto(): ArticleResponseDto = ArticleResponseDto(
    slug = slug!!,
    title = title!!,
    description = description!!,
    body = body,
    tagList = tagList,
    createdAt = Instant.fromEpochMilliseconds(createdAt!!).toString(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt!!).toString(),
    favorited = favorited,
    favoritesCount = favoritesCount,
    author = author!!.toUserResponseDto()
)

fun List<Article>.toArticlesResponseDto(): ArticlesResponseDto =
    ArticlesResponseDto(
        articles = this.map { it.toArticleResponseDto() },
        articlesCount = this.size
    )

fun User.toUserResponseDto(): UserResponseDto =
    UserResponseDto(
        username = this.username!!,
        email = this.email,
        bio = this.bio,
        image = this.image
    )
