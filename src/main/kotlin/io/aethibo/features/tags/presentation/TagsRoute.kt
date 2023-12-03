package io.aethibo.features.tags.presentation

import io.aethibo.core.navigation.Tags
import io.aethibo.features.tags.domain.controller.TagsController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.tags(tagController: TagsController) {
    route(Tags.route) {
        authenticate("jwt", optional = true) {
            get { tagController.get(this.context) }
        }
    }
}
