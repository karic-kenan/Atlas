package io.aethibo.features.users.presentation.navigation

import io.aethibo.features.users.data.failure.getErrorMessage
import io.aethibo.features.users.data.failure.toHttpStatus
import io.aethibo.features.users.domain.mapper.toDomain
import io.aethibo.features.users.domain.mapper.toUserResponseDto
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.usecase.UpdateUserUseCase
import io.aethibo.features.users.presentation.model.UpdateUserWrapper
import io.aethibo.features.users.presentation.model.UserWrapperResponseDto
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.updateUser(
    updateUserUseCase: UpdateUserUseCase
) {
    val request = call.receive<UpdateUserWrapper>()
    val email = call.authentication.principal<User>()?.email
    val result = updateUserUseCase(email, request.user.toDomain())

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { updated ->
            call.respond(
                status = HttpStatusCode.OK,
                message = UserWrapperResponseDto(updated.toUserResponseDto())
            )
        }
    )
}
