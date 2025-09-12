package io.aethibo.features.articles.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.articles.domain.usecase.*
import io.aethibo.features.articles.presentation.model.*
import io.aethibo.features.articles.presentation.navigation.*
import io.github.smiley4.ktoropenapi.resources.delete
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.post
import io.github.smiley4.ktoropenapi.resources.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.article(
    findFeedUseCase: FindFeedUseCase,
    findArticlesUseCase: FindArticlesUseCase,
    getArticleBySlugUseCase: GetArticleBySlugUseCase,
    createArticleUseCase: CreateArticleUseCase,
    updateArticleUseCase: UpdateArticleUseCase,
    deleteArticleUseCase: DeleteArticleUseCase,
    favouriteArticleUseCase: FavoriteArticleUseCase,
    unfavoriteArticleUseCase: UnfavoriteArticleUseCase,
) {
    authenticate("jwt-access") {
        // GET /api/articles/feed
        get<Api.Articles.Feed>({
            description = "Get user's personalized article feed from followed authors"
            summary = "Get article feed"
            tags = setOf("Articles", "Social")
            request {
                queryParameter<Int>("limit") {
                    description = "Number of articles to return (max 100)"
                    example("limit example") {
                        value = 20
                    }
                    required = false
                }
                queryParameter<Long>("offset") {
                    description = "Number of articles to skip for pagination"
                    example("offset example") {
                        value = 0
                    }
                    required = false
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "List of articles from followed users"
                    body<ArticlesResponseDto> {
                        example("Successful Feed Response") {
                            ArticlesResponseDto(
                                articles = listOf(
                                    ArticleResponseDto(
                                        slug = "how-to-train-your-dragon-123",
                                        title = "How to Train Your Dragon",
                                        description = "A comprehensive guide to dragon training",
                                        body = "# Dragon Training 101\n\nTraining dragons requires patience and understanding...",
                                        tagList = listOf("dragons", "fantasy", "tutorial"),
                                        createdAt = "2025-01-15T10:30:00Z",
                                        updatedAt = "2025-01-15T10:30:00Z",
                                        favorited = false,
                                        favoritesCount = 42,
                                        author = UserResponseDto(
                                            username = "dragon_master",
                                            email = "dragon@example.com",
                                            bio = "Professional dragon trainer with 10+ years experience",
                                            image = "https://example.com/avatar.jpg"
                                        )
                                    )
                                ),
                                articlesCount = 1
                            )
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            mapOf("error" to "Authentication token required")
                        }
                    }
                }
                HttpStatusCode.Forbidden to {
                    description = "Access forbidden"
                    body<Map<String, String>> {
                        example("Forbidden") {
                            mapOf("error" to "Insufficient permissions")
                        }
                    }
                }
            }
        }) { resource ->
            getFeed(resource.limit, resource.offset, findFeedUseCase)
        }

        // POST /api/articles
        post<Api.CreateArticle>({
            description = "Create a new article with title, description, body and optional tags"
            summary = "Create new article"
            tags = setOf("Articles")
            request {
                body<CreateArticleWrapper> {
                    description = "Article data to create"
                    example("New Article") {
                        CreateArticleWrapper(
                            article = CreateArticleRequest(
                                title = "Introduction to Ktor",
                                description = "Learn how to build APIs with Ktor framework",
                                body = "# Getting Started with Ktor\n\nKtor is a framework for building asynchronous servers and clients in connected systems using the powerful Kotlin programming language...",
                                tagList = listOf("kotlin", "ktor", "backend", "api")
                            )
                        )
                    }
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Article created successfully"
                    body<ArticleResponseDto> {
                        example("Created Article") {
                            ArticleResponseDto(
                                slug = "introduction-to-ktor-456",
                                title = "Introduction to Ktor",
                                description = "Learn how to build APIs with Ktor framework",
                                body = "# Getting Started with Ktor\n\nKtor is a framework for building asynchronous servers...",
                                tagList = listOf("kotlin", "ktor", "backend", "api"),
                                createdAt = "2025-01-15T11:00:00Z",
                                updatedAt = "2025-01-15T11:00:00Z",
                                favorited = false,
                                favoritesCount = 0,
                                author = UserResponseDto(
                                    username = "kotlin_dev",
                                    email = "dev@example.com",
                                    bio = "Full-stack Kotlin developer",
                                    image = null
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid request data or validation errors"
                    body<Map<String, String>> {
                        example("Validation Error") {
                            mapOf("error" to "Title cannot be empty")
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            mapOf("error" to "Authentication token required")
                        }
                    }
                }
                HttpStatusCode.UnprocessableEntity to {
                    description = "Request data validation failed"
                    body<Map<String, String>> {
                        example("Validation Failed") {
                            mapOf("error" to "Article with this title already exists")
                        }
                    }
                }
            }
        }) { _ ->
            createArticle(createArticleUseCase)
        }

        // GET /api/articles/{slug}
        get<Api.Articles.Slug>({
            description = "Get a single article by its unique slug identifier"
            summary = "Get article by slug"
            tags = setOf("Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Unique article identifier (URL-friendly version of title)"
                    example("slug example") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Article retrieved successfully"
                    body<ArticleResponseDto> {
                        example("Article Details") {
                            ArticleResponseDto(
                                slug = "how-to-train-your-dragon-123",
                                title = "How to Train Your Dragon",
                                description = "A comprehensive guide to dragon training",
                                body = "# Dragon Training 101\n\nTraining dragons requires patience and understanding. This guide will walk you through the essential steps...",
                                tagList = listOf("dragons", "fantasy", "tutorial"),
                                createdAt = "2025-01-15T10:30:00Z",
                                updatedAt = "2025-01-15T10:30:00Z",
                                favorited = true,
                                favoritesCount = 42,
                                author = UserResponseDto(
                                    username = "dragon_master",
                                    email = "dragon@example.com",
                                    bio = "Professional dragon trainer",
                                    image = "https://example.com/avatar.jpg"
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>> {
                        example("Not Found") {
                            mapOf("error" to "Article with slug 'invalid-slug' not found")
                        }
                    }
                }
            }
        }) { resource ->
            getArticle(resource.slug, getArticleBySlugUseCase)
        }

        // PUT /api/articles/{slug}
        put<Api.Articles.Slug>({
            description = "Update an existing article. Only the author can update their own articles."
            summary = "Update article"
            tags = setOf("Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Unique article identifier"
                    example("slug example") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
                body<UpdateArticleRequest> {
                    description = "Article fields to update (all fields are optional)"
                    example("Update Article") {
                        UpdateArticleRequest(
                            title = "How to Train Your Dragon - Updated Edition",
                            description = "An updated and comprehensive guide to dragon training",
                            body = null, // Keep existing body
                            tagList = listOf("dragons", "fantasy", "tutorial", "updated")
                        )
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Article updated successfully"
                    body<ArticleResponseDto>()
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid request data"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Forbidden to {
                    description = "Cannot update article - not the author"
                    body<Map<String, String>> {
                        example("Forbidden") {
                            mapOf("error" to "You can only update your own articles")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>>()
                }
            }
        }) { resource ->
            updateArticle(resource.slug, updateArticleUseCase)
        }

        // DELETE /api/articles/{slug}
        delete<Api.Articles.Slug>({
            description = "Delete an article permanently. Only the author can delete their own articles."
            summary = "Delete article"
            tags = setOf("Articles")
            request {
                pathParameter<String>("slug") {
                    description = "Unique article identifier"
                    example("slug example") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
            }
            response {
                HttpStatusCode.NoContent to {
                    description = "Article deleted successfully"
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Forbidden to {
                    description = "Cannot delete article - not the author"
                    body<Map<String, String>> {
                        example("Forbidden") {
                            mapOf("error" to "You can only delete your own articles")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>>()
                }
            }
        }) { resource ->
            deleteArticle(resource.slug, deleteArticleUseCase)
        }

        // POST /api/articles/{slug}/favorite
        post<Api.Articles.Slug.Favorite>({
            description = "Add article to user's favorites list"
            summary = "Favorite article"
            tags = setOf("Articles", "Social")
            request {
                pathParameter<String>("slug") {
                    description = "Unique article identifier"
                    example("slug example") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Article favorited successfully"
                    body<ArticleResponseDto> {
                        example("Favorited Article") {
                            ArticleResponseDto(
                                slug = "how-to-train-your-dragon-123",
                                title = "How to Train Your Dragon",
                                description = "A comprehensive guide to dragon training",
                                body = "# Dragon Training 101...",
                                tagList = listOf("dragons", "fantasy", "tutorial"),
                                createdAt = "2025-01-15T10:30:00Z",
                                updatedAt = "2025-01-15T10:30:00Z",
                                favorited = true,
                                favoritesCount = 43, // Incremented
                                author = UserResponseDto(
                                    username = "dragon_master",
                                    email = "dragon@example.com",
                                    bio = "Professional dragon trainer",
                                    image = "https://example.com/avatar.jpg"
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Conflict to {
                    description = "Article already favorited"
                    body<Map<String, String>> {
                        example("Already Favorited") {
                            mapOf("error" to "Article is already in your favorites")
                        }
                    }
                }
            }
        }) { resource ->
            favoriteArticle(resource.parent.slug, favouriteArticleUseCase)
        }

        // DELETE /api/articles/{slug}/favorite
        delete<Api.Articles.Slug.Favorite>({
            description = "Remove article from user's favorites list"
            summary = "Unfavorite article"
            tags = setOf("Articles", "Social")
            request {
                pathParameter<String>("slug") {
                    description = "Unique article identifier"
                    example("slug example") {
                        value = "how-to-train-your-dragon-123"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Article unfavorited successfully"
                    body<ArticleResponseDto>()
                }
                HttpStatusCode.NotFound to {
                    description = "Article not found"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Conflict to {
                    description = "Article was not favorited"
                    body<Map<String, String>> {
                        example("Not Favorited") {
                            mapOf("error" to "Article is not in your favorites")
                        }
                    }
                }
            }
        }) { resource ->
            unfavoriteArticle(resource.parent.slug, unfavoriteArticleUseCase)
        }
    }

    authenticate("jwt-access", optional = true) {
        // GET /api/articles with query parameters
        get<Api.Articles>({
            description = """
                Get multiple articles with optional filtering and pagination.
                Authentication is optional - if authenticated, articles will include user-specific data like 'favorited' status.
                If not authenticated, 'favorited' will always be false.
                """.trimIndent()
            summary = "List articles with filtering"
            tags = setOf("Articles")
            request {
                queryParameter<String>("tag") {
                    description = "Filter articles by tag"
                    example("tag example") {
                        value = "kotlin"
                    }
                    required = false
                }
                queryParameter<String>("author") {
                    description = "Filter articles by author username"
                    example("username example") {
                        value = "john_doe"
                    }
                    required = false
                }
                queryParameter<String>("favorited") {
                    description = "Filter by articles favorited by this username"
                    example("favorited example") {
                        value = "jane_doe"
                    }
                    required = false
                }
                queryParameter<Int>("limit") {
                    description = "Number of articles to return (default: 20, max: 100)"
                    example("limit example") {
                        value = 20
                    }
                    required = false
                }
                queryParameter<Long>("offset") {
                    description = "Number of articles to skip for pagination"
                    example("offset example") {
                        value = 0
                    }
                    required = false
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "List of articles matching the specified criteria"
                    body<ArticlesResponseDto> {
                        example("Filtered Articles") {
                            ArticlesResponseDto(
                                articles = listOf(
                                    ArticleResponseDto(
                                        slug = "kotlin-coroutines-guide-789",
                                        title = "Kotlin Coroutines Guide",
                                        description = "Master asynchronous programming in Kotlin",
                                        body = "# Kotlin Coroutines\n\nCoroutines are a Kotlin feature that converts async callbacks for long-running tasks...",
                                        tagList = listOf("kotlin", "coroutines", "async"),
                                        createdAt = "2025-01-14T14:20:00Z",
                                        updatedAt = "2025-01-14T14:20:00Z",
                                        favorited = false,
                                        favoritesCount = 15,
                                        author = UserResponseDto(
                                            username = "kotlin_expert",
                                            email = "expert@example.com",
                                            bio = "Kotlin advocate and educator",
                                            image = "https://example.com/kotlin-expert.jpg"
                                        )
                                    )
                                ),
                                articlesCount = 1
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid query parameters"
                    body<Map<String, String>> {
                        example("Bad Request") {
                            mapOf("error" to "Limit cannot exceed 100")
                        }
                    }
                }
            }
        }) { resource ->
            val request = FindArticlesRequest(
                tag = resource.tag,
                author = resource.author,
                favourite = resource.favorited,
                limit = resource.limit,
                offset = resource.offset
            )
            findArticles(request, findArticlesUseCase)
        }
    }
}
