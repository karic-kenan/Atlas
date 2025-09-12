package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Api
import io.aethibo.features.users.domain.usecase.AuthenticateUserUseCase
import io.aethibo.features.users.domain.usecase.CreateUserUseCase
import io.aethibo.features.users.domain.usecase.UpdateUserUseCase
import io.aethibo.features.users.presentation.model.*
import io.aethibo.features.users.presentation.navigation.authenticateUser
import io.aethibo.features.users.presentation.navigation.createUser
import io.aethibo.features.users.presentation.navigation.getCurrentUser
import io.aethibo.features.users.presentation.navigation.updateUser
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.post
import io.github.smiley4.ktoropenapi.resources.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.user(
    authenticateUserUseCase: AuthenticateUserUseCase,
    createUserUseCase: CreateUserUseCase,
    updateUserUseCase: UpdateUserUseCase
) {
    // POST /api/users - User Registration
    post<Api.Users>({
        description = "Register a new user account with email, username and password"
        summary = "Register new user"
        tags = setOf("Authentication", "Users")
        request {
            body<RegisterUserWrapper> {
                description = "User registration data"
                example("Register User") {
                    value = RegisterUserWrapper(
                        user = RegisterUserRequest(
                            email = "john.doe@example.com",
                            password = "securePassword123",
                            username = "john_doe"
                        )
                    )
                }
            }
        }
        response {
            HttpStatusCode.Created to {
                description = "User registered successfully with authentication tokens"
                body<UserCreatedWrapperResponseDto> {
                    example("User Created") {
                        value = UserCreatedWrapperResponseDto(
                            user = UserCreatedResponseDto(
                                id = 1,
                                email = "john.doe@example.com",
                                username = "john_doe",
                                bio = null,
                                image = null,
                                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                refreshToken = "rt_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                createdAt = "2025-01-15T12:00:00Z",
                                updatedAt = "2025-01-15T12:00:00Z"
                            )
                        )
                    }
                }
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid registration data or validation errors"
                body<Map<String, String>> {
                    example("Validation Error") {
                        value = mapOf("error" to "Email is required and must be valid")
                    }
                }
            }
            HttpStatusCode.Conflict to {
                description = "User already exists"
                body<Map<String, String>> {
                    example("User Exists") {
                        value = mapOf("error" to "User with this email already exists")
                    }
                }
            }
            HttpStatusCode.UnprocessableEntity to {
                description = "Registration data validation failed"
                body<Map<String, String>> {
                    example("Weak Password") {
                        value = mapOf("error" to "Password must be at least 8 characters long")
                    }
                }
            }
        }
    }) { resource ->
        createUser(createUserUseCase)
    }

    // POST /api/users/login - User Authentication
    post<Api.Users.Login>({
        description = "Authenticate user with email and password to obtain access tokens"
        summary = "User login"
        tags = setOf("Authentication")
        request {
            body<LoginUserWrapper> {
                description = "User login credentials"
                example("Login User") {
                    value = LoginUserWrapper(
                        user = LoginUserRequest(
                            email = "john.doe@example.com",
                            password = "securePassword123"
                        )
                    )
                }
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Authentication successful with tokens"
                body<UserWrapperResponseDto> {
                    example("Login Success") {
                        value = UserWrapperResponseDto(
                            user = UserResponseDto(
                                id = 1,
                                email = "john.doe@example.com",
                                username = "john_doe",
                                bio = "Software developer passionate about Kotlin",
                                image = "https://example.com/avatars/john_doe.jpg",
                                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                refreshToken = "rt_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                createdAt = "2025-01-15T12:00:00Z",
                                updatedAt = "2025-01-15T12:00:00Z",
                                isActive = true,
                                emailVerified = true,
                                lastLoginAt = "2025-01-15T14:30:00Z",
                                failedLoginAttempts = 0,
                                lockedUntil = null
                            )
                        )
                    }
                }
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid login data"
                body<Map<String, String>> {
                    example("Missing Email") {
                        value = mapOf("error" to "Email and password are required")
                    }
                }
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid credentials"
                body<Map<String, String>> {
                    example("Invalid Credentials") {
                        value = mapOf("error" to "Invalid email or password")
                    }
                }
            }
            HttpStatusCode.Forbidden to {
                description = "Account locked or inactive"
                body<Map<String, String>> {
                    example("Account Locked") {
                        value = mapOf("error" to "Account is temporarily locked due to too many failed login attempts")
                    }
                }
            }
            HttpStatusCode.TooManyRequests to {
                description = "Too many login attempts"
                body<Map<String, String>> {
                    example("Rate Limited") {
                        value = mapOf("error" to "Too many login attempts. Please try again later.")
                    }
                }
            }
        }
    }) { resource ->
        authenticateUser(authenticateUserUseCase)
    }

    authenticate("jwt-access") {
        // GET /api/user - Get Current User
        get<Api.User>({
            description = "Get the current authenticated user's profile information"
            summary = "Get current user"
            tags = setOf("Users")
            response {
                HttpStatusCode.OK to {
                    description = "Current user information retrieved successfully"
                    body<UserWrapperResponseDto> {
                        example("Current User") {
                            value = UserWrapperResponseDto(
                                user = UserResponseDto(
                                    id = 1,
                                    email = "john.doe@example.com",
                                    username = "john_doe",
                                    bio = "Software developer passionate about Kotlin",
                                    image = "https://example.com/avatars/john_doe.jpg",
                                    token = null, // Token not included in profile responses
                                    refreshToken = null,
                                    createdAt = "2025-01-15T12:00:00Z",
                                    updatedAt = "2025-01-15T14:30:00Z",
                                    isActive = true,
                                    emailVerified = true,
                                    lastLoginAt = "2025-01-15T14:30:00Z",
                                    failedLoginAttempts = 0,
                                    lockedUntil = null
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication token missing or invalid"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Valid authentication token required")
                        }
                    }
                }
                HttpStatusCode.NotFound to {
                    description = "User not found"
                    body<Map<String, String>> {
                        example("User Not Found") {
                            value = mapOf("error" to "User account not found")
                        }
                    }
                }
            }
        }) { resource ->
            getCurrentUser()
        }

        // PUT /api/user - Update Current User
        put<Api.User>({
            description = "Update the current authenticated user's profile information. All fields are optional."
            summary = "Update current user"
            tags = setOf("Users")
            request {
                body<UpdateUserWrapper> {
                    description = "User profile updates (all fields optional)"
                    example("Update User") {
                        value = UpdateUserWrapper(
                            user = UpdateUserRequest(
                                email = "john.doe.updated@example.com",
                                username = "john_doe_updated",
                                bio = "Senior Software Developer passionate about Kotlin and clean architecture",
                                image = "https://example.com/avatars/john_doe_new.jpg"
                            )
                        )
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User updated successfully"
                    body<UserUpdatedWrapperResponseDto> {
                        example("Updated User") {
                            value = UserUpdatedWrapperResponseDto(
                                user = UserUpdatedResponseDto(
                                    id = 1,
                                    email = "john.doe.updated@example.com",
                                    username = "john_doe_updated",
                                    bio = "Senior Software Developer passionate about Kotlin and clean architecture",
                                    image = "https://example.com/avatars/john_doe_new.jpg",
                                    createdAt = "2025-01-15T12:00:00Z",
                                    updatedAt = "2025-01-15T15:00:00Z"
                                )
                            )
                        }
                    }
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid update data"
                    body<Map<String, String>> {
                        example("Invalid Email") {
                            value = mapOf("error" to "Invalid email format")
                        }
                    }
                }
                HttpStatusCode.Unauthorized to {
                    description = "Authentication required"
                    body<Map<String, String>> {
                        example("Unauthorized") {
                            value = mapOf("error" to "Valid authentication token required")
                        }
                    }
                }
                HttpStatusCode.Conflict to {
                    description = "Username or email already taken"
                    body<Map<String, String>> {
                        example("Username Taken") {
                            value = mapOf("error" to "Username is already taken")
                        }
                    }
                }
                HttpStatusCode.UnprocessableEntity to {
                    description = "Validation errors"
                    body<Map<String, String>> {
                        example("Validation Error") {
                            value = mapOf("error" to "Username must be between 3 and 30 characters")
                        }
                    }
                }
            }
        }) { resource ->
            updateUser(updateUserUseCase)
        }
    }
}
