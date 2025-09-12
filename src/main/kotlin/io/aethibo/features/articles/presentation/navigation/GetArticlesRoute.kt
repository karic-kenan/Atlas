package io.aethibo.features.articles.presentation.navigation

import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticlesResponseDto
import io.aethibo.features.articles.domain.usecase.FindArticlesUseCase
import io.aethibo.features.articles.presentation.model.FindArticlesRequest
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.findArticles(
    findArticlesRequest: FindArticlesRequest,
    findArticlesUseCase: FindArticlesUseCase
) {
    val result = findArticlesUseCase(
        findArticlesRequest.limit,
        findArticlesRequest.offset,
        findArticlesRequest.tag,
        findArticlesRequest.author,
        findArticlesRequest.favourite
    )

    result.fold(
        ifLeft = { failure ->
            val statusCode = failure.toHttpStatus()
            val errorMessage = failure.getErrorMessage()
            call.respond(
                status = statusCode,
                message = mapOf("error" to errorMessage)
            )
        },
        ifRight = { articles ->
            call.respond(
                status = HttpStatusCode.OK,
                message = articles.toArticlesResponseDto()
            )
        }
    )
}
