package io.aethibo.features.profiles.presentation

import io.aethibo.features.profiles.domain.controller.ProfilesController
import io.aethibo.features.users.domain.model.ProfileDto
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.service.UsersService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

class ProfilesControllerImpl(private val userService: UsersService) : ProfilesController {
    override suspend fun get(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val username = call.parameters["username"]
        val profile = userService.getProfileByUsername(email, username)

        call.respond(ProfileDto(profile))
    }

    override suspend fun follow(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val usernameToFollow = call.parameters["username"]
        require(!usernameToFollow.isNullOrBlank()) { "User does not exist" }

        val profile = userService.follow(email, usernameToFollow)
        call.respond(ProfileDto(profile))
    }

    override suspend fun unfollow(call: ApplicationCall) {
        val email = call.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged in." }

        val usernameToUnfollow = call.parameters["username"]
        require(!usernameToUnfollow.isNullOrBlank()) { "User does not exist" }

        val profile = userService.unfollow(email, usernameToUnfollow)
        call.respond(ProfileDto(profile))
    }
}
