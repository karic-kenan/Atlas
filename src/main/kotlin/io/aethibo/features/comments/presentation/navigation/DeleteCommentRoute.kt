package io.aethibo.features.comments.presentation.navigation

import io.aethibo.core.utils.CommentId
import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.getErrorMessage
import io.aethibo.features.comments.data.failure.toHttpStatus
import io.aethibo.features.comments.domain.usecase.DeleteCommentUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.deleteComment(
    slug: Slug,
    commentId: CommentId,
    deleteCommentUseCase: DeleteCommentUseCase
) {
    val result = deleteCommentUseCase(commentId, slug)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = {
            call.respond(HttpStatusCode.NoContent)
        }
    )
}
