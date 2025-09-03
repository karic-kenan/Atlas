package io.aethibo.core.config

import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.articles.presentation.articles
import io.aethibo.features.comments.domain.controller.CommentsController
import io.aethibo.features.profiles.domain.controller.ProfilesController
import io.aethibo.features.profiles.presentation.profiles
import io.aethibo.features.tags.domain.controller.TagsController
import io.aethibo.features.tags.presentation.tags
import io.aethibo.features.users.domain.controller.UsersController
import io.aethibo.features.users.presentation.users
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val userController: UsersController by inject()
    val profileController: ProfilesController by inject()
    val articleController: ArticlesController by inject()
    val commentController: CommentsController by inject()
    val tagController: TagsController by inject()

    routing {
        users(userController)
        profiles(profileController)
        articles(articleController, commentController)
        tags(tagController)
    }
}
