package io.aethibo.features.comments.domain.controller

import io.ktor.server.application.*

interface CommentsController {
    suspend fun add(call: ApplicationCall)
    suspend fun findBySlug(call: ApplicationCall)
    suspend fun delete(call: ApplicationCall)
}
