package io.aethibo.features.articles.presentation

import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.articles.domain.model.ArticleDTO
import io.aethibo.features.articles.domain.model.ArticlesDTO
import io.aethibo.features.articles.domain.service.ArticlesService
import io.aethibo.features.users.domain.model.User
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class ArticlesControllerImpl(private val articleService: ArticlesService) : ArticlesController {
    override suspend fun findBy(call: ApplicationCall) {
        val tag = call.parameters["tag"]
        val author = call.parameters["author"]
        val favorited = call.parameters["favorited"]
        val limit = call.parameters["limit"] ?: "20"
        val offset = call.parameters["offset"] ?: "0"

        val articles = articleService.findBy(tag, author, favorited, limit.toInt(), offset.toLong())
        call.respond(ArticlesDTO(articles, articles.size))
    }

    override suspend fun feed(call: ApplicationCall) {
        val limit = call.parameters["limit"] ?: "20"
        val offset = call.parameters["offset"] ?: "0"

        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val feedArticles = articleService.findFeed(email, limit.toInt(), offset.toLong())
        call.respond(ArticlesDTO(feedArticles, feedArticles.size))
    }

    override suspend fun get(call: ApplicationCall) {
        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        val article = articleService.findBySlug(slug)
        call.respond(ArticleDTO(article))
    }

    override suspend fun create(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val articleDto = call.receive<ArticleDTO>()
        val article = articleService.create(email, articleDto.validArticle())
        call.respond(ArticleDTO(article))
    }

    override suspend fun update(call: ApplicationCall) {
        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        val articleDto = call.receive<ArticleDTO>()
        require(!articleDto.article?.body.isNullOrBlank()) { "Body is null" }

        val article = articleService.update(slug, articleDto.validArticle())
        call.respond(ArticleDTO(article))
    }

    override suspend fun delete(call: ApplicationCall) {
        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        articleService.delete(slug)
    }

    override suspend fun favorite(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        val article = articleService.favorite(email, slug)
        call.respond(ArticleDTO(article))
    }

    override suspend fun unfavorite(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        val article = articleService.unfavorite(email, slug)
        call.respond(ArticleDTO(article))
    }
}
