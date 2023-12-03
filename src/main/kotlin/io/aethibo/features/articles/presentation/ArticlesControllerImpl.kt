package io.aethibo.features.articles.presentation

import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.articles.domain.model.Article
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
        articleService.findBy(tag, author, favorited, limit.toInt(), offset.toLong()).also { articles ->
            call.respond(ArticlesDTO(articles, articles.size))
        }
    }

    override suspend fun feed(call: ApplicationCall) {
        val limit = call.parameters["limit"] ?: "20"
        val offset = call.parameters["offset"] ?: "0"
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        articleService.findFeed(email, limit.toInt(), offset.toLong()).also { articles ->
            call.respond(ArticlesDTO(articles, articles.size))
        }
    }

    override suspend fun get(call: ApplicationCall) {
        call.parameters["slug"].also { slug ->
            require(!slug.isNullOrBlank()) { "slug must not be blank" }

            articleService.findBySlug(slug).also { article: Article ->
                call.respond(ArticleDTO(article))
            }
        }
    }

    override suspend fun create(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        call.receive<ArticleDTO>().apply {
            articleService.create(email, this.validArticle()).apply {
                call.respond(ArticleDTO(this))
            }
        }
    }

    override suspend fun update(call: ApplicationCall) {
        val slug = call.parameters["slug"]
        require(!slug.isNullOrBlank()) { "slug must not be blank" }

        call.receive<ArticleDTO>().apply {
            require(!article?.body.isNullOrBlank()) { "Body is null" }

            articleService.update(slug, this.validArticle()).apply {
                call.respond(ArticleDTO(this))
            }
        }
    }

    override suspend fun delete(call: ApplicationCall) {
        call.parameters["slug"].also { slug ->
            require(!slug.isNullOrBlank()) { "slug must not be blank" }

            articleService.delete(slug)
        }
    }

    override suspend fun favorite(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        call.parameters["slug"].also { slug ->
            require(!slug.isNullOrBlank()) { "slug must not be blank" }

            articleService.favorite(email, slug).apply {
                call.respond(ArticleDTO(this))
            }
        }
    }

    override suspend fun unfavorite(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        call.parameters["slug"].also { slug ->
            require(!slug.isNullOrBlank()) { "slug must not be blank" }

            articleService.unfavorite(email, slug).apply {
                call.respond(ArticleDTO(this))
            }
        }
    }
}
