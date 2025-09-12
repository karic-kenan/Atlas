package io.aethibo.features.comments.presentation.navigation

import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.getErrorMessage
import io.aethibo.features.comments.data.failure.toHttpStatus
import io.aethibo.features.comments.domain.mapper.toCommentResponseDto
import io.aethibo.features.comments.domain.mapper.toDomain
import io.aethibo.features.comments.domain.usecase.CreateCommentUseCase
import io.aethibo.features.comments.presentation.model.CreateCommentWrapper
import io.aethibo.features.users.domain.model.User
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.createComment(
    slug: Slug,
    createCommentUseCase: CreateCommentUseCase
) {
    val request = call.receive<CreateCommentWrapper>()
    val email = call.authentication.principal<User>()?.email

    val result = createCommentUseCase(
        email,
        slug,
        request.comment.toDomain()
    )

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { comment ->
            call.respond(
                status = HttpStatusCode.Created,
                message = mapOf("comment" to comment.toCommentResponseDto())
            )
        }
    )
}
