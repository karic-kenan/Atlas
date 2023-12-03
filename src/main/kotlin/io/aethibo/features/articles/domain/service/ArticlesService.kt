package io.aethibo.features.articles.domain.service

import io.aethibo.features.articles.domain.model.Article

interface ArticlesService {
    suspend fun findBy(tag: String?, author: String?, favorited: String?, limit: Int, offset: Long): List<Article>
    suspend fun create(email: String?, article: Article): Article
    suspend fun findBySlug(slug: String): Article
    suspend fun update(slug: String, article: Article): Article?
    suspend fun findFeed(email: String?, limit: Int, offset: Long): List<Article>
    suspend fun favorite(email: String?, slug: String): Article
    suspend fun unfavorite(email: String?, slug: String): Article
    suspend fun delete(slug: String)
}
