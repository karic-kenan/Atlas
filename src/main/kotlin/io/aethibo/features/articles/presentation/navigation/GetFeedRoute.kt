package io.aethibo.features.articles.presentation.navigation

import io.aethibo.core.utils.Limit
import io.aethibo.core.utils.Offset
import io.aethibo.features.articles.data.failure.getErrorMessage
import io.aethibo.features.articles.data.failure.toHttpStatus
import io.aethibo.features.articles.domain.mapper.toArticlesResponseDto
import io.aethibo.features.articles.domain.usecase.FindFeedUseCase
import io.aethibo.features.users.domain.model.User
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.getFeed(
    limit: Limit,
    offset: Offset,
    findFeedUseCase: FindFeedUseCase
) {
    val email = call.authentication.principal<User>()?.email
    val result = findFeedUseCase(email, limit, offset)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
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
