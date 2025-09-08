package io.aethibo.features.tags.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.tags.domain.usecase.GetAllTagsUseCase
import io.aethibo.features.tags.presentation.navigation.getAllTags
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*

fun Route.tag(
    getAllTagsUseCase: GetAllTagsUseCase,
) {
    authenticate("jwt", optional = true) {
        get<Api.Tags> { resource ->
            getAllTags(getAllTagsUseCase)
        }
    }
}
