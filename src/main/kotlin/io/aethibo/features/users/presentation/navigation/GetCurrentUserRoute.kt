package io.aethibo.features.users.presentation.navigation

import arrow.core.Either
import io.aethibo.features.users.domain.mapper.toUserResponseDto
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.presentation.model.UserWrapperResponseDto
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.getCurrentUser() {
    val user = call.authentication.principal<User>()

    val result = Either.catch {
        requireNotNull(user) { "User not authenticated" }
        user
    }

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = mapOf("error" to (failure.message ?: "Unauthorized"))
            )
        },
        ifRight = { user ->
            call.respond(
                status = HttpStatusCode.OK,
                message = UserWrapperResponseDto(user.toUserResponseDto())
            )
        }
    )
}
