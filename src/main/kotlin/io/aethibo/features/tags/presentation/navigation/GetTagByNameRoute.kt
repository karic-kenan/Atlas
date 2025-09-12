package io.aethibo.features.tags.presentation.navigation

import io.aethibo.features.tags.data.failure.getErrorMessage
import io.aethibo.features.tags.data.failure.toHttpStatus
import io.aethibo.features.tags.domain.usecase.GetTagByNameUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.getTagByName(
    name: String,
    getTagByNameUseCase: GetTagByNameUseCase
) {
    val result = getTagByNameUseCase(name)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { tag ->
            if (tag != null) {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = mapOf("tag" to tag)
                )
            } else {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = mapOf("error" to "Tag '$name' not found")
                )
            }
        }
    )
}
