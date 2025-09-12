package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.users.domain.usecase.FollowProfileUseCase
import io.aethibo.features.users.domain.usecase.GetProfileByUsernameUseCase
import io.aethibo.features.users.domain.usecase.UnfollowProfileUseCase
import io.aethibo.features.users.presentation.model.ProfileResponseDto
import io.aethibo.features.users.presentation.model.ProfileWrapperResponseDto
import io.aethibo.features.users.presentation.navigation.followUser
import io.aethibo.features.users.presentation.navigation.getProfileByUsernameRoute
import io.aethibo.features.users.presentation.navigation.unfollowUser
import io.github.smiley4.ktoropenapi.resources.delete
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.profile(
    getProfileByUsernameUseCase: GetProfileByUsernameUseCase,
    followProfileUseCase: FollowProfileUseCase,
    unfollowProfileUseCase: UnfollowProfileUseCase,
) {
    authenticate("jwt-access") {
        // POST /api/profiles/celeb_{USERNAME}/follow - Follow User
        post<Api.Profile.Follow>({
            description = "Follow another user to see their articles in your feed"
            summary = "Follow user"
            tags = setOf("Social", "Users")
            request {
                pathParameter<String>("username") {
                    description = "Username of the user to follow"
                    example("Follow Username") {
                        value = "dragon_master"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User followed successfully"
                    body<ProfileWrapperResponseDto> {
                        example("Followed Profile") {
                            value = ProfileWrapperResponseDto(
                                profile = ProfileResponseDto(
                                    username = "dragon_master",
                                    bio = "Professional dragon trainer with 10+ years experience",
                                    image = "https://example.com/avatars/dragon_master.jpg",
                                    following = true
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid request or attempting to follow yourself"
                    body<Map<String, String>> {
                        example("Self Follow") {
                            value = mapOf("error" to "You cannot follow yourself")
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Authentication token required")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "User to follow not found"
                    body<Map<String, String>> {
                        example("User Not Found") {
                            value = mapOf("error" to "User with username 'nonexistent' not found")
                        }
                    }
                }
                HttpStatusCode.Conflict to {
                    description = "Already following this user"
                    body<Map<String, String>> {
                        example("Already Following") {
                            value = mapOf("error" to "You are already following this user")
                        }
                    }
                }
            }
        }) { resources ->
            followUser(resources.parent.username, followProfileUseCase)
        }

        // DELETE /api/profiles/celeb_{USERNAME}/follow - Unfollow User
        delete<Api.Profile.Follow>({
            description = "Stop following a user and remove their articles from your feed"
            summary = "Unfollow user"
            tags = setOf("Social", "Users")
            request {
                pathParameter<String>("username") {
                    description = "Username of the user to unfollow"
                    example("Unfollow Username") {
                        value = "dragon_master"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User unfollowed successfully"
                    body<ProfileWrapperResponseDto> {
                        example("Unfollowed Profile") {
                            value = ProfileWrapperResponseDto(
                                profile = ProfileResponseDto(
                                    username = "dragon_master",
                                    bio = "Professional dragon trainer with 10+ years experience",
                                    image = "https://example.com/avatars/dragon_master.jpg",
                                    following = false
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid request or attempting to unfollow yourself"
                    body<Map<String, String>> {
                        example("Self Unfollow") {
                            value = mapOf("error" to "You cannot unfollow yourself")
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Authentication token required")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "User to unfollow not found"
                    body<Map<String, String>> {
                        example("User Not Found") {
                            value = mapOf("error" to "User with username 'nonexistent' not found")
                        }
                    }
                }
                HttpStatusCode.Conflict to {
                    description = "Not following this user"
                    body<Map<String, String>> {
                        example("Not Following") {
                            value = mapOf("error" to "You are not following this user")
                        }
                    }
                }
            }
        }) { resources ->
            unfollowUser(resources.parent.username, unfollowProfileUseCase)
        }
    }

    authenticate("jwt-access", optional = true) {
        // GET /api/profiles/celeb_{USERNAME} - Get User Profile
        get<Api.Profile>({
            description = """
                Get a user's public profile information including their bio, image, and follow status.
                If authenticated, the response will include whether you're following this user.
                If not authenticated, 'following' will always be false.
            """.trimIndent()
            summary = "Get user profile"
            tags = setOf("Users", "Social")
            request {
                pathParameter<String>("username") {
                    description = "Username of the user whose profile to retrieve"
                    example("Profile Username") {
                        value = "dragon_master"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User profile retrieved successfully"
                    body<ProfileWrapperResponseDto> {
                        example("User Profile") {
                            value = ProfileWrapperResponseDto(
                                profile = ProfileResponseDto(
                                    username = "dragon_master",
                                    bio = "Professional dragon trainer with 10+ years experience. Author of 'Dragon Training 101' and 'Advanced Dragon Psychology'.",
                                    image = "https://example.com/avatars/dragon_master.jpg",
                                    following = true
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "User profile not found"
                    body<Map<String, String>> {
                        example("Profile Not Found") {
                            value = mapOf("error" to "User with username 'nonexistent' not found")
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid username format"
                    body<Map<String, String>> {
                        example("Invalid Username") {
                            value = mapOf("error" to "Username contains invalid characters")
                        }
                    }
                }
            }
        }) { resources ->
            getProfileByUsernameRoute(resources.username, getProfileByUsernameUseCase)
        }
    }
}
