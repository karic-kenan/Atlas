package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class GetProfileByUsernameUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockUserRepository = mockk<UsersRepository>()

    val useCase = GetProfileByUsernameUseCase { email, username ->
        getProfileByUsername(mockUserRepository, email, username)
    }

    test("should return profile when username and email are valid") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val validUsername = "testuser"
            val userId = 1L

            val mockUser = User(
                id = userId,
                email = "test@example.com",
                username = validUsername,
                password = "hashedpassword",
                bio = "Test bio",
                image = "test-image.jpg"
            )

            coEvery { mockUserRepository.findByUsername(validUsername) } returns mockUser
            coEvery { mockUserRepository.findIsFollowUser(validEmail, userId) } returns true

            // Act
            val result = useCase(validEmail, validUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Profile>>()
            val profile = result.value
            profile.username shouldBe validUsername
            profile.bio shouldBe "Test bio"
            profile.image shouldBe "test-image.jpg"
            profile.following shouldBe true
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val validUsername = "testuser"

            // Act
            val result = useCase(emptyEmail, validUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            failure.email shouldBe ""
        }
    }

    test("should return failure when username is empty") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val emptyUsername = ""

            // Act
            val result = useCase(validEmail, emptyUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.InvalidUsername>()
            failure.username shouldBe ""
        }
    }

    test("should return failure when user is not found") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val nonExistentUsername = "nonexistent"

            coEvery { mockUserRepository.findByUsername(nonExistentUsername) } returns null

            // Act
            val result = useCase(validEmail, nonExistentUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.UserNotFoundByUsername>()
            failure.username shouldBe nonExistentUsername
        }
    }

    test("should handle UserException from repository") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val validUsername = "testuser"

            coEvery { mockUserRepository.findByUsername(validUsername) } throws
                    UserException.DatabaseError("find by username", Exception("DB connection failed"))

            // Act
            val result = useCase(validEmail, validUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.DatabaseError>()
        }
    }

    test("should handle generic exception from repository") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val validUsername = "testuser"

            coEvery { mockUserRepository.findByUsername(validUsername) } throws
                    RuntimeException("Unexpected error")

            // Act
            val result = useCase(validEmail, validUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.DatabaseError>()
            failure.operation shouldBe "get profile by username"
        }
    }

    test("should handle null email gracefully") {
        runTest {
            // Arrange
            val nullEmail: String? = null
            val validUsername = "testuser"

            // Act
            val result = useCase(nullEmail, validUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            failure.email shouldBe ""
        }
    }
})