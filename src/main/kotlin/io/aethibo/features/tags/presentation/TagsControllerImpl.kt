package io.aethibo.features.tags.presentation

import io.aethibo.features.tags.domain.controller.TagsController
import io.aethibo.features.tags.domain.service.TagsService
import io.ktor.server.application.*
import io.ktor.server.response.*

class TagsControllerImpl(private val tagService: TagsService) : TagsController {
    override suspend fun get(ctx: ApplicationCall) =
        tagService.findAll().also { tagDto ->
            ctx.respond(tagDto)
        }
}
