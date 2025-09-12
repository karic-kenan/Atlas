package io.aethibo.features.tags.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.tags.domain.usecase.GetAllTagsUseCase
import io.aethibo.features.tags.presentation.model.TagsResponseDto
import io.aethibo.features.tags.presentation.navigation.getAllTags
import io.github.smiley4.ktoropenapi.resources.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.tag(getAllTagsUseCase: GetAllTagsUseCase) {
    authenticate("jwt-access", optional = true) {
        // GET /api/tags - Get All Tags
        get<Api.Tags>({
            description = """
                Get all available tags used across articles in the system.
                Tags are returned as a simple list of strings, sorted alphabetically.
                Authentication is optional - the same tags are returned for all users.
                This endpoint is useful for displaying tag suggestions or filters.
            """.trimIndent()
            summary = "Get all tags"
            tags = setOf("Tags", "Articles")
            response {
                HttpStatusCode.OK to {
                    description = "Tags retrieved successfully"
                    body<TagsResponseDto> {
                        example("Available Tags") {
                            value = TagsResponseDto(
                                tags = listOf(
                                    "api",
                                    "async",
                                    "backend",
                                    "coroutines",
                                    "dragons",
                                    "fantasy",
                                    "kotlin",
                                    "ktor",
                                    "programming",
                                    "tutorial",
                                    "web-development"
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.InternalServerError to {
                    description = "Server error while retrieving tags"
                    body<Map<String, String>> {
                        example("Server Error") {
                            value = mapOf("error" to "Unable to retrieve tags at this time")
                        }
                    }
                }
            }
        }) { resource ->
            getAllTags(getAllTagsUseCase)
        }
    }
}
