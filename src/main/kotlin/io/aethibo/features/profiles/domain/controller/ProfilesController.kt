package io.aethibo.features.profiles.domain.controller

import io.ktor.server.application.*

interface ProfilesController {
    suspend fun get(call: ApplicationCall)
    suspend fun follow(call: ApplicationCall)
    suspend fun unfollow(call: ApplicationCall)
}
