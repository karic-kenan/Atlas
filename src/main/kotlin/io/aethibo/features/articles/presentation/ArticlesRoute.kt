package io.aethibo.features.articles.presentation

import io.aethibo.core.navigation.*
import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.comments.domain.controller.CommentsController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.articles(articleController: ArticlesController, commentController: CommentsController) {
    route(Articles.route) {
        authenticate("jwt") {
            get(Feed.route) { articleController.feed(this.context) }
            route(Slug.route) {
                route(Comments.route) {
                    post { commentController.add(this.context) }
                    authenticate("jwt", optional = true) {
                        get { commentController.findBySlug(this.context) }
                    }
                    delete(CommentId.route) { commentController.delete(this.context) }
                }
                route(Favorite.route) {
                    post { articleController.favorite(this.context) }
                    delete { articleController.unfavorite(this.context) }
                }
                get { articleController.get(this.context) }
                put { articleController.update(this.context) }
                delete { articleController.delete(this.context) }
            }
            authenticate("jwt", optional = true) {
                get { articleController.findBy(this.context) }
            }
            post { articleController.create(this.context) }
        }
    }
}
