package io.aethibo.features.users.presentation

import io.aethibo.features.users.domain.controller.UsersController
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.model.UserDTO
import io.aethibo.features.users.domain.service.UsersService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class UsersControllerImpl(private val userService: UsersService) : UsersController {
    override suspend fun getUserByEmail(email: String?): User {
        require(!email.isNullOrBlank()) { "User not logged or with invalid email." }
        return userService.getByEmail(email)
    }

    override suspend fun register(call: ApplicationCall) {
        val userResponse = call.receive<UserDTO>()
        val isValidUserToRegister = userResponse.validRegister()
        val user = userService.create(isValidUserToRegister)

        call.respond(UserDTO(user))
    }

    override suspend fun login(call: ApplicationCall) {
        val userResponse = call.receive<UserDTO>()
        val isValidUserToLogin = userResponse.validLogin()
        val user = userService.authenticate(isValidUserToLogin)

        call.respond(UserDTO(user))
    }

    override suspend fun getCurrent(call: ApplicationCall) {
        call.respond(UserDTO(call.authentication.principal()))
    }

    override suspend fun update(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User is not logged in." }

        val userResponse = call.receive<UserDTO>()
        val isValidUserToUpdate = userResponse.validToUpdate()
        val user = userService.update(email, isValidUserToUpdate)

        call.respond(UserDTO(user))
    }
}
