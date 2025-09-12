package io.aethibo.features.users.presentation.navigation

import io.aethibo.core.utils.Username
import io.aethibo.features.users.data.failure.getErrorMessage
import io.aethibo.features.users.data.failure.toHttpStatus
import io.aethibo.features.users.domain.mapper.toProfileResponseDto
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.usecase.UnfollowProfileUseCase
import io.aethibo.features.users.presentation.model.ProfileWrapperResponseDto
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.unfollowUser(
    username: Username,
    unfollowProfileUseCase: UnfollowProfileUseCase,
) {
    val email = call.authentication.principal<User>()?.email
    val result = unfollowProfileUseCase(email, username)

    result.fold(
        ifLeft = { failure ->
            call.respond(
                status = failure.toHttpStatus(),
                message = mapOf("error" to failure.getErrorMessage())
            )
        },
        ifRight = { profile ->
            call.respond(
                status = HttpStatusCode.Created,
                message = ProfileWrapperResponseDto(profile.toProfileResponseDto())
            )
        }
    )
}