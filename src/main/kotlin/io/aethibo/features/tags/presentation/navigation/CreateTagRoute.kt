package io.aethibo.features.tags.presentation.navigation

import io.aethibo.features.tags.data.failure.getErrorMessage
import io.aethibo.features.tags.data.failure.toHttpStatus
import io.aethibo.features.tags.domain.usecase.CreateTagUseCase
import io.aethibo.features.tags.presentation.model.CreateTagRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.createTag(
    createTagUseCase: CreateTagUseCase
) {
    val request = call.receive<CreateTagRequest>()
    val result = createTagUseCase(request.name)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { tag ->
            call.respond(
                status = HttpStatusCode.Created,
                message = mapOf("tag" to tag)
            )
        }
    )
}
