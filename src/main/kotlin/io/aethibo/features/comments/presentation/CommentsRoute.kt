package io.aethibo.features.comments.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.articles.presentation.model.UserResponseDto
import io.aethibo.features.comments.domain.usecase.CreateCommentUseCase
import io.aethibo.features.comments.domain.usecase.DeleteCommentUseCase
import io.aethibo.features.comments.domain.usecase.FindCommentsUseCase
import io.aethibo.features.comments.presentation.model.CommentResponseDto
import io.aethibo.features.comments.presentation.model.CommentsResponseDto
import io.aethibo.features.comments.presentation.model.CreateCommentRequest
import io.aethibo.features.comments.presentation.model.CreateCommentWrapper
import io.aethibo.features.comments.presentation.navigation.createComment
import io.aethibo.features.comments.presentation.navigation.deleteComment
import io.aethibo.features.comments.presentation.navigation.findComments
import io.github.smiley4.ktoropenapi.resources.delete
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.comment(
    findCommentsUseCase: FindCommentsUseCase,
    createCommentUseCase: CreateCommentUseCase,
    deleteCommentUseCase: DeleteCommentUseCase,
) {
    authenticate("jwt-access") {
        // POST /api/articles/{slug}/comments - Create Comment
        post<Api.Articles.Slug.Comments>({
            description = "Add a new comment to an article. Authentication required."
            summary = "Create comment"
            tags = setOf("Comments", "Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Article slug to comment on"
                    example("Article Slug") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
                body<CreateCommentWrapper> {
                    description = "Comment content to add"
                    example("New Comment") {
                        value = CreateCommentWrapper(
                            comment = CreateCommentRequest(
                                body = "Great article! I learned a lot about dragon training techniques."
                            )
                        )
                    }
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Comment created successfully"
                    body<CommentResponseDto> {
                        example("Created Comment") {
                            value = CommentResponseDto(
                                id = 1,
                                body = "Great article! I learned a lot about dragon training techniques.",
                                createdAt = "2025-01-15T16:30:00Z",
                                updatedAt = "2025-01-15T16:30:00Z",
                                author = UserResponseDto(
                                    email = "reader@example.com",
                                    username = "avid_reader",
                                    bio = "Love reading about fantasy and technology",
                                    image = "https://example.com/avatars/reader.jpg",
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid comment data"
                    body<Map<String, String>> {
                        example("Empty Comment") {
                            value = mapOf("error" to "Comment body cannot be empty")
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Authentication token required to comment")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>> {
                        example("Article Not Found") {
                            value = mapOf("error" to "Article with slug 'invalid-slug' not found")
                        }
                    }
                }
                HttpStatusCode.UnprocessableEntity to {
                    description = "Comment validation failed"
                    body<Map<String, String>> {
                        example("Comment Too Long") {
                            value = mapOf("error" to "Comment body cannot exceed 1000 characters")
                        }
                    }
                }
            }
        }) { resource ->
            createComment(resource.parent.slug, createCommentUseCase)
        }

        // DELETE /api/articles/{slug}/comments/{commentId} - Delete Comment
        delete<Api.Articles.Slug.Comments.CommentId>({
            description = "Delete a comment from an article. Only the comment author can delete their own comments."
            summary = "Delete comment"
            tags = setOf("Comments", "Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Article slug containing the comment"
                    example("Article Slug") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
                pathParameter<Long>("commentId") {
                    description = "Unique identifier of the comment to delete"
                    example("Comment ID") {
                        value = 1L
                    }
                }
            }
            response {
                HttpStatusCode.NoContent to {
                    description = "Comment deleted successfully"
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Authentication token required")
                        }
                    }
                }
                HttpStatusCode.Forbidden to {
                    description = "Cannot delete comment - not the author"
                    body<Map<String, String>> {
                        example("Not Author") {
                            value = mapOf("error" to "You can only delete your own comments")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article or comment not found"
                    body<Map<String, String>> {
                        example("Comment Not Found") {
                            value = mapOf("error" to "Comment with ID 999 not found on this article")
                        }
                    }
                }
            }
        }) { resource ->
            deleteComment(resource.parent.parent.slug, resource.commentId, deleteCommentUseCase)
        }
    }

    authenticate("jwt-access", optional = true) {
        // GET /api/articles/{slug}/comments - Get Article Comments
        get<Api.Articles.Slug.Comments>({
            description = """
                Get all comments for an article, ordered by creation date (newest first).
                Authentication is optional - if authenticated, user-specific data may be included in responses.
            """.trimIndent()
            summary = "Get article comments"
            tags = setOf("Comments", "Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Article slug to get comments for"
                    example("Article Slug") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Comments retrieved successfully"
                    body<CommentsResponseDto> {
                        example("Article Comments") {
                            value = CommentsResponseDto(
                                comments = listOf(
                                    CommentResponseDto(
                                        id = 2,
                                        body = "This is exactly what I needed! Thank you for sharing your expertise.",
                                        createdAt = "2025-01-15T17:00:00Z",
                                        updatedAt = "2025-01-15T17:00:00Z",
                                        author = UserResponseDto(
                                            email = "enthusiast@example.com",
                                            username = "dragon_enthusiast",
                                            bio = "Dragon enthusiast and mythology researcher",
                                            image = "https://example.com/avatars/enthusiast.jpg",
                                        )
                                    ),
                                    CommentResponseDto(
                                        id = 1,
                                        body = "Great article! I learned a lot about dragon training techniques.",
                                        createdAt = "2025-01-15T16:30:00Z",
                                        updatedAt = "2025-01-15T16:30:00Z",
                                        author = UserResponseDto(
                                            email = "reader@example.com",
                                            username = "avid_reader",
                                            bio = "Love reading about fantasy and technology",
                                            image = "https://example.com/avatars/reader.jpg",
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>> {
                        example("Article Not Found") {
                            value = mapOf("error" to "Article with slug 'invalid-slug' not found")
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid article slug format"
                    body<Map<String, String>> {
                        example("Invalid Slug") {
                            value = mapOf("error" to "Invalid article slug format")
                        }
                    }
                }
            }
        }) { resource ->
            findComments(resource.parent.slug, findCommentsUseCase)
        }
    }
}
