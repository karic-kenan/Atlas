package io.aethibo.features.articles.presentation.navigation

import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticleResponseDto
import io.aethibo.features.articles.domain.mapper.toDomain
import io.aethibo.features.articles.domain.usecase.UpdateArticleUseCase
import io.aethibo.features.articles.presentation.model.UpdateArticleRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.updateArticle(
    slug: Slug,
    updateArticleUseCase: UpdateArticleUseCase
) {
    val request = call.receive<UpdateArticleRequest>()
    val result = updateArticleUseCase(slug, request.toDomain())

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
