package io.aethibo.features.tags.domain.controller

import io.aethibo.features.tags.domain.model.TagDTO
import io.ktor.server.application.*

interface TagsController {
    suspend fun get(call: ApplicationCall): TagDTO
}
