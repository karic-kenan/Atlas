package io.aethibo.features.articles.domain.repository

import io.aethibo.features.articles.domain.model.Article

interface ArticlesRepository {
    suspend fun findByTag(tag: String, limit: Int, offset: Long): List<Article>
    suspend fun findByFavorited(favorited: String, limit: Int, offset: Long): List<Article>
    suspend fun create(article: Article): Article?
    suspend fun findAll(limit: Int, offset: Long): List<Article>
    suspend fun findFeed(email: String, limit: Int, offset: Long): List<Article>
    suspend fun findBySlug(slug: String): Article?
    suspend fun findByAuthor(author: String, limit: Int, offset: Long): List<Article>
    suspend fun update(slug: String, article: Article): Article?
    suspend fun favorite(userId: Long, slug: String): Long
    suspend fun unfavorite(userId: Long, slug: String): Long
    suspend fun delete(slug: String)
}
