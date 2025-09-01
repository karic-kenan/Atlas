package io.aethibo.features.profiles.presentation

import io.aethibo.core.navigation.Follow
import io.aethibo.core.navigation.Profile
import io.aethibo.features.profiles.domain.controller.ProfilesController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.profiles(profileController: ProfilesController) {
    route(Profile.route) {
        authenticate("jwt", optional = true) {
            get { profileController.get(call) }
        }
        authenticate("jwt") {
            route(Follow.route) {
                post { profileController.follow(call) }
                delete { profileController.unfollow(call) }
            }
        }
    }
}
