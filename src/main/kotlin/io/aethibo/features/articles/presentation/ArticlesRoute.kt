package io.aethibo.features.articles.presentation

import io.aethibo.core.navigation.*
import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.comments.domain.controller.CommentsController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.articles(articleController: ArticlesController, commentController: CommentsController) {
    route(Articles.route) {
        authenticate("jwt") {
            get(Feed.route) { articleController.feed(call) }
            route(Slug.route) {
                route(Comments.route) {
                    post { commentController.add(call) }
                    authenticate("jwt", optional = true) {
                        get { commentController.findBySlug(call) }
                    }
                    delete(CommentId.route) { commentController.delete(call) }
                }
                route(Favorite.route) {
                    post { articleController.favorite(call) }
                    delete { articleController.unfavorite(call) }
                }
                get { articleController.get(call) }
                put { articleController.update(call) }
                delete { articleController.delete(call) }
            }
            authenticate("jwt", optional = true) {
                get { articleController.findBy(call) }
            }
            post { articleController.create(call) }
        }
    }
}
