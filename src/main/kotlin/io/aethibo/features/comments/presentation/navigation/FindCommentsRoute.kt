package io.aethibo.features.comments.presentation.navigation

import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.getErrorMessage
import io.aethibo.features.comments.data.failure.toHttpStatus
import io.aethibo.features.comments.domain.mapper.toCommentsResponseDto
import io.aethibo.features.comments.domain.usecase.FindCommentsUseCase
import io.aethibo.features.comments.presentation.model.CommentsResponseDto
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.findComments(
    slug: Slug,
    findCommentsBySlugUseCase: FindCommentsUseCase
) {
    val result = findCommentsBySlugUseCase(slug)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { comments ->
            call.respond(
                status = HttpStatusCode.OK,
                message = CommentsResponseDto(
                    comments = comments.toCommentsResponseDto()
                )
            )
        }
    )
}
