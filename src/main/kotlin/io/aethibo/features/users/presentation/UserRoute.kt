package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.users.domain.usecase.AuthenticateUserUseCase
import io.aethibo.features.users.domain.usecase.CreateUserUseCase
import io.aethibo.features.users.domain.usecase.UpdateUserUseCase
import io.aethibo.features.users.presentation.navigation.authenticateUser
import io.aethibo.features.users.presentation.navigation.createUser
import io.aethibo.features.users.presentation.navigation.getCurrentUser
import io.aethibo.features.users.presentation.navigation.updateUser
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.Route

fun Route.user(
    authenticateUserUseCase: AuthenticateUserUseCase,
    createUserUseCase: CreateUserUseCase,
    updateUserUseCase: UpdateUserUseCase
) {
    // POST /api/users
    post<Api.Users> { resource ->
        createUser(createUserUseCase)
    }

    // POST /api/users/login/
    post<Api.Users.Login> { resource ->
        authenticateUser(authenticateUserUseCase)
    }

    authenticate("jwt-access") {
        // GET /api/user
        get<Api.User> { resource ->
            getCurrentUser()
        }

        // PUT /api/user
        put<Api.User> { resource ->
            updateUser(updateUserUseCase)
        }
    }
}
