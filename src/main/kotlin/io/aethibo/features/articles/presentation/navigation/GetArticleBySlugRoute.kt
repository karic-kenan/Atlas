package io.aethibo.features.articles.presentation.navigation

import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticleResponseDto
import io.aethibo.features.articles.domain.usecase.GetArticleBySlugUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.getArticle(
    slug: Slug,
    getArticleBySlugUseCase: GetArticleBySlugUseCase
) {
    val result = getArticleBySlugUseCase(slug)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { article ->
            call.respond(
                status = HttpStatusCode.OK,
                message = article.toArticleResponseDto()
            )
        }
    )
}
