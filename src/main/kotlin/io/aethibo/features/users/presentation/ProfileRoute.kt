package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.users.domain.usecase.FollowProfileUseCase
import io.aethibo.features.users.domain.usecase.GetProfileByUsernameUseCase
import io.aethibo.features.users.domain.usecase.UnfollowProfileUseCase
import io.aethibo.features.users.presentation.navigation.followUser
import io.aethibo.features.users.presentation.navigation.getProfileByUsernameRoute
import io.aethibo.features.users.presentation.navigation.unfollowUser
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.routing.*

fun Route.profile(
    getProfileByUsernameUseCase: GetProfileByUsernameUseCase,
    followProfileUseCase: FollowProfileUseCase,
    unfollowProfileUseCase: UnfollowProfileUseCase,
) {
    authenticate("jwt-access") {
        // POST /api/profiles/celeb_{USERNAME}/follow
        post<Api.Profile.Follow> { resources ->
            followUser(resources.parent.username, followProfileUseCase)
        }

        // DELETE /api/profiles/celeb_{USERNAME}/follow
        delete<Api.Profile.Follow> { resources ->
            unfollowUser(resources.parent.username, unfollowProfileUseCase)
        }
    }

    authenticate("jwt-access", optional = true) {
        // GET /api/profiles/celeb_{{USERNAME}}
        get<Api.Profile> { resources ->
            getProfileByUsernameRoute(resources.username, getProfileByUsernameUseCase)
        }
    }
}
