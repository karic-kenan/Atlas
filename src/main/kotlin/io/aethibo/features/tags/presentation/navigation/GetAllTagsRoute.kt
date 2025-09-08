package io.aethibo.features.tags.presentation.navigation

import io.aethibo.features.tags.data.failure.getErrorMessage
import io.aethibo.features.tags.data.failure.toHttpStatus
import io.aethibo.features.tags.domain.usecase.GetAllTagsUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.getAllTags(
    getAllTagsUseCase: GetAllTagsUseCase
) {
    val result = getAllTagsUseCase()

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { tags ->
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf("tags" to tags)
            )
        }
    )
}
