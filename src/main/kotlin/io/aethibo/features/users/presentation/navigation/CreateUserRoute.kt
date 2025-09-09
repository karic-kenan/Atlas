package io.aethibo.features.users.presentation.navigation

import io.aethibo.features.users.data.failure.getErrorMessage
import io.aethibo.features.users.data.failure.toHttpStatus
import io.aethibo.features.users.domain.mapper.toCreatedResponseDto
import io.aethibo.features.users.domain.mapper.toDomain
import io.aethibo.features.users.domain.usecase.CreateUserUseCase
import io.aethibo.features.users.presentation.model.RegisterUserWrapper
import io.aethibo.features.users.presentation.model.UserCreatedWrapperResponseDto
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.createUser(
    createUserUseCase: CreateUserUseCase
) {
    val request = call.receive<RegisterUserWrapper>()
    val result = createUserUseCase(request.user.toDomain())

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { user ->
            call.respond(
                status = HttpStatusCode.Created,
                message = UserCreatedWrapperResponseDto(user.toCreatedResponseDto())
            )
        }
    )
}
