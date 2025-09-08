package io.aethibo.features.tags.presentation.navigation

import io.aethibo.features.tags.data.failure.getErrorMessage
import io.aethibo.features.tags.data.failure.toHttpStatus
import io.aethibo.features.tags.domain.usecase.CheckTagExistsUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.checkTagExists(
    name: String,
    checkTagExistsUseCase: CheckTagExistsUseCase
) {
    val result = checkTagExistsUseCase(name)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { exists ->
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf(
                    "name" to name,
                    "exists" to exists
                )
            )
        }
    )
}
