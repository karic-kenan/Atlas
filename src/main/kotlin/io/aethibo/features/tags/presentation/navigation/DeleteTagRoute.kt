package io.aethibo.features.tags.presentation.navigation

import io.aethibo.features.tags.data.failure.getErrorMessage
import io.aethibo.features.tags.data.failure.toHttpStatus
import io.aethibo.features.tags.domain.usecase.DeleteTagUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.deleteTag(
    name: String,
    deleteTagUseCase: DeleteTagUseCase
) {
    val result = deleteTagUseCase(name)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = {
            call.respond(message = HttpStatusCode.NoContent)
        }
    )
}