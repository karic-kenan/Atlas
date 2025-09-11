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

class UnfollowProfileUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockUserRepository = mockk<UsersRepository>()

    val useCase = UnfollowProfileUseCase { email, username ->
        unfollowProfile(mockUserRepository, email, username)
    }

    test("should unfollow user successfully") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val targetUsername = "userToUnfollow"
            val unfollowedUser = User(
                id = 1,
                email = "userToUnfollow@example.com",
                username = targetUsername,
                bio = "Test bio",
                image = "https://example.com/image.jpg",
                password = "hashedPassword"
            )

            coEvery { mockUserRepository.unfollow(validEmail, targetUsername) } returns unfollowedUser

            // Act
            val result = useCase(validEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Profile>>()
            val profile = result.value
            profile.username shouldBe targetUsername
            profile.following shouldBe false
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val targetUsername = "userToUnfollow"

            // Act
            val result = useCase(emptyEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
        }
    }

    test("should return failure when username is empty") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val emptyUsername = ""

            // Act
            val result = useCase(validEmail, emptyUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidUsername>()
        }
    }

    test("should handle NotFollowing exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val targetUsername = "userToUnfollow"

            coEvery { mockUserRepository.unfollow(validEmail, targetUsername) } throws
                    UserException.NotFollowing(validEmail, targetUsername)

            // Act
            val result = useCase(validEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.NotFollowing>()
        }
    }

    test("should handle UserNotFoundByUsername exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val nonExistentUsername = "nonExistentUser"

            coEvery { mockUserRepository.unfollow(validEmail, nonExistentUsername) } throws
                    UserException.UserNotFoundByUsername(nonExistentUsername)

            // Act
            val result = useCase(validEmail, nonExistentUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserNotFoundByUsername>()
        }
    }

    test("should handle UserNotFoundByEmail exception") {
        runTest {
            // Arrange
            val invalidEmail = "invalid@example.com"
            val targetUsername = "userToUnfollow"

            coEvery { mockUserRepository.unfollow(invalidEmail, targetUsername) } throws
                    UserException.UserNotFoundByEmail(invalidEmail)

            // Act
            val result = useCase(invalidEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserNotFoundByEmail>()
        }
    }

    test("should handle SelfFollowAttempt exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val sameUsername = "sameUser"

            coEvery { mockUserRepository.unfollow(validEmail, sameUsername) } throws
                    UserException.SelfFollowAttempt(validEmail)

            // Act
            val result = useCase(validEmail, sameUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.SelfFollowAttempt>()
        }
    }

    test("should handle UnfollowOperationFailed exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val targetUsername = "userToUnfollow"

            coEvery { mockUserRepository.unfollow(validEmail, targetUsername) } throws
                    UserException.UnfollowOperationFailed(validEmail, targetUsername)

            // Act
            val result = useCase(validEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UnfollowOperationFailed>()
        }
    }

    test("should handle generic database errors") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val targetUsername = "userToUnfollow"

            coEvery { mockUserRepository.unfollow(validEmail, targetUsername) } throws
                    Exception("Database connection lost")

            // Act
            val result = useCase(validEmail, targetUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.DatabaseError>()
        }
    }
})