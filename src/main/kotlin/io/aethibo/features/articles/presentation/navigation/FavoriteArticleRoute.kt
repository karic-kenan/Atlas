package io.aethibo.features.articles.presentation.navigation

import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticleResponseDto
import io.aethibo.features.articles.domain.usecase.FavoriteArticleUseCase
import io.aethibo.features.users.domain.model.User
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.favoriteArticle(
    slug: String,
    favoriteArticleUseCase: FavoriteArticleUseCase
) {
    val email = call.authentication.principal<User>()?.email
    val result = favoriteArticleUseCase(email, slug)

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
