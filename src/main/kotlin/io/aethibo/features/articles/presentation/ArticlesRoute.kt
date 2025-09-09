package io.aethibo.features.articles.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.articles.domain.usecase.CreateArticleUseCase
import io.aethibo.features.articles.domain.usecase.DeleteArticleUseCase
import io.aethibo.features.articles.domain.usecase.FavoriteArticleUseCase
import io.aethibo.features.articles.domain.usecase.FindArticlesUseCase
import io.aethibo.features.articles.domain.usecase.FindFeedUseCase
import io.aethibo.features.articles.domain.usecase.GetArticleBySlugUseCase
import io.aethibo.features.articles.domain.usecase.UnfavoriteArticleUseCase
import io.aethibo.features.articles.domain.usecase.UpdateArticleUseCase
import io.aethibo.features.articles.presentation.model.FindArticlesRequest
import io.aethibo.features.articles.presentation.navigation.*
import io.aethibo.features.articles.presentation.navigation.createArticle
import io.aethibo.features.articles.presentation.navigation.favoriteArticle
import io.aethibo.features.articles.presentation.navigation.updateArticle
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.Route

fun Route.article(
    findFeedUseCase: FindFeedUseCase,
    findArticlesUseCase: FindArticlesUseCase,
    getArticleBySlugUseCase: GetArticleBySlugUseCase,
    createArticleUseCase: CreateArticleUseCase,
    updateArticleUseCase: UpdateArticleUseCase,
    deleteArticleUseCase: DeleteArticleUseCase,
    favouriteArticleUseCase: FavoriteArticleUseCase,
    unfavoriteArticleUseCase: UnfavoriteArticleUseCase,
) {
    authenticate("jwt-access") {
        // GET /api/articles/feed
        get<Api.Articles.Feed> { resource ->
            getFeed(resource.limit, resource.offset, findFeedUseCase)
        }

        // POST /api/articles
        post<Api.CreateArticle> { _ ->
            createArticle(createArticleUseCase)
        }

        // GET /api/articles/{slug}
        get<Api.Articles.Slug> { resource ->
            getArticle(resource.slug, getArticleBySlugUseCase)
        }

        // PUT /api/articles/{slug}
        put<Api.Articles.Slug> { resource ->
            updateArticle(resource.slug, updateArticleUseCase)
        }

        // DELETE /api/articles/{slug}
        delete<Api.Articles.Slug> { resource ->
            deleteArticle(resource.slug, deleteArticleUseCase)
        }

        // POST /api/articles/{slug}/favorite
        post<Api.Articles.Slug.Favorite> { resource ->
            favoriteArticle(resource.parent.slug, favouriteArticleUseCase)
        }

        // DELETE /api/articles/{slug}/favorite
        delete<Api.Articles.Slug.Favorite> { resource ->
            unfavoriteArticle(resource.parent.slug, unfavoriteArticleUseCase)
        }
    }

    authenticate("jwt-access", optional = true) {
        // GET /api/articles with query parameters
        get<Api.Articles> { resource ->
            val request = FindArticlesRequest(
                tag = resource.tag,
                author = resource.author,
                favourite = resource.favorited,
                limit = resource.limit,
                offset = resource.offset
            )
            findArticles(request, findArticlesUseCase)
        }
    }
}
