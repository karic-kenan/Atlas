package io.aethibo.features.users.presentation.navigation

import io.aethibo.features.users.data.failure.getErrorMessage
import io.aethibo.features.users.data.failure.toHttpStatus
import io.aethibo.features.users.domain.mapper.toDomain
import io.aethibo.features.users.domain.mapper.toUserResponseDto
import io.aethibo.features.users.domain.usecase.AuthenticateUserUseCase
import io.aethibo.features.users.presentation.model.LoginUserWrapper
import io.aethibo.features.users.presentation.model.UserWrapperResponseDto
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.authenticateUser(
    authenticateUserUseCase: AuthenticateUserUseCase
) {
    val request = call.receive<LoginUserWrapper>()
    val result = authenticateUserUseCase(request.user.toDomain())

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
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
