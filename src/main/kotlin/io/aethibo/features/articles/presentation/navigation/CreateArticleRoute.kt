package io.aethibo.features.articles.presentation.navigation

import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticleResponseDto
import io.aethibo.features.articles.domain.mapper.toDomain
import io.aethibo.features.articles.domain.usecase.CreateArticleUseCase
import io.aethibo.features.articles.presentation.model.CreateArticleWrapper
import io.aethibo.features.users.domain.model.User
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.createArticle(
    createArticleUseCase: CreateArticleUseCase
) {
    val request = call.receive<CreateArticleWrapper>()
    val email = call.authentication.principal<User>()?.email

    val result = createArticleUseCase(
        email,
        request.article.toDomain()
    )

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { article ->
            call.respond(
                status = HttpStatusCode.Created,
                message = article.toArticleResponseDto()
            )
        }
    )
}
