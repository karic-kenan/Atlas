package io.aethibo.features.articles.domain.repository

import io.aethibo.core.utils.Limit
import io.aethibo.core.utils.Offset
import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.domain.model.Article

interface ArticleRepository {
    // Create
    suspend fun create(article: Article): Article?

    // Read
    suspend fun all(limit: Limit, offset: Offset): List<Article>
    suspend fun getBySlug(slug: Slug): Article?
    suspend fun getByAuthor(author: String, limit: Limit, offset: Offset): List<Article>
    suspend fun getByTag(tag: String, limit: Limit, offset: Offset): List<Article>
    suspend fun getByFavourite(favourite: String, limit: Limit, offset: Offset): List<Article>
    suspend fun getFeed(email: String, limit: Limit, offset: Offset): List<Article>

    // Update
    suspend fun update(slug: Slug, article: Article): Article?

    // Delete
    suspend fun delete(slug: Slug)

    // Special actions
    suspend fun favorite(userId: Long, slug: Slug): Long
    suspend fun unfavorite(userId: Long, slug: Slug): Long
}
