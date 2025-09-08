package io.aethibo.features.comments.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.comments.domain.usecase.CreateCommentUseCase
import io.aethibo.features.comments.domain.usecase.DeleteCommentUseCase
import io.aethibo.features.comments.domain.usecase.FindCommentsUseCase
import io.aethibo.features.comments.presentation.navigation.createComment
import io.aethibo.features.comments.presentation.navigation.deleteComment
import io.aethibo.features.comments.presentation.navigation.findComments
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.routing.*

fun Route.comment(
    findCommentsUseCase: FindCommentsUseCase,
    createCommentUseCase: CreateCommentUseCase,
    deleteCommentUseCase: DeleteCommentUseCase,
) {
    authenticate("jwt") {
        // POST /api/articles/{slug}/comments
        post<Api.Articles.Slug.Comments> { resource ->
            createComment(resource.parent.slug, createCommentUseCase)
        }

        // DELETE /api/articles/{slug}/comments/{commentId}
        delete<Api.Articles.Slug.Comments.CommentId> { resource ->
            deleteComment(resource.parent.parent.slug, resource.commentId, deleteCommentUseCase)
        }
    }

    authenticate("jwt", optional = true) {
        // GET /api/articles/{slug}/comments
        get<Api.Articles.Slug.Comments> { resource ->
            findComments(resource.parent.slug, findCommentsUseCase)
        }
    }
}
