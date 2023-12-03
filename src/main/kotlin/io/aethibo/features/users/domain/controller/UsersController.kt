package io.aethibo.features.users.domain.controller

import io.aethibo.features.users.domain.model.User
import io.ktor.server.application.*

interface UsersController {
    suspend fun getUserByEmail(email: String?): User
    suspend fun register(call: ApplicationCall)
    suspend fun login(call: ApplicationCall)
    suspend fun getCurrent(call: ApplicationCall)
    suspend fun update(call: ApplicationCall)
}
