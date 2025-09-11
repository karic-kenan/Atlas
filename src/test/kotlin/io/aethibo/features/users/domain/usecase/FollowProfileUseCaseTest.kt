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

class FollowProfileUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockUserRepository = mockk<UsersRepository>()

    val useCase = FollowProfileUseCase { email, username ->
        followProfile(mockUserRepository, email, username)
    }

    test("should follow user successfully and return profile with following=true") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val usernameToFollow = "target_user"

            val followedUser = User(
                id = 1,
                email = "target@example.com",
                username = usernameToFollow,
                password = "hashedpassword",
                bio = "Test bio",
                image = "https://example.com/image.jpg"
            )

            coEvery {
                mockUserRepository.follow(validEmail, usernameToFollow)
            } returns followedUser

            // Act
            val result = useCase(validEmail, usernameToFollow)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Profile>>()
            val profile = result.value
            profile.username shouldBe usernameToFollow
            profile.bio shouldBe "Test bio"
            profile.image shouldBe "https://example.com/image.jpg"
            profile.following shouldBe true
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val usernameToFollow = "target_user"

            // Act
            val result = useCase(emptyEmail, usernameToFollow)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            failure.email shouldBe ""
        }
    }

    test("should return failure when username to follow is empty") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
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

    test("should handle UserException from repository") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val usernameToFollow = "target_user"

            coEvery {
                mockUserRepository.follow(validEmail, usernameToFollow)
            } throws UserException.SelfFollowAttempt(validEmail)

            // Act
            val result = useCase(validEmail, usernameToFollow)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.SelfFollowAttempt>()
            failure.email shouldBe validEmail
        }
    }

    test("should handle AlreadyFollowing exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val usernameToFollow = "target_user"

            coEvery {
                mockUserRepository.follow(validEmail, usernameToFollow)
            } throws UserException.AlreadyFollowing(validEmail, usernameToFollow)

            // Act
            val result = useCase(validEmail, usernameToFollow)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.AlreadyFollowing>()
            failure.apply {
                followerEmail shouldBe validEmail
                targetUsername shouldBe usernameToFollow
            }
        }
    }

    test("should handle generic database errors") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val usernameToFollow = "target_user"
            val dbException = RuntimeException("Database connection error")

            coEvery {
                mockUserRepository.follow(validEmail, usernameToFollow)
            } throws dbException

            // Act
            val result = useCase(validEmail, usernameToFollow)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.DatabaseError>()
            failure.apply {
                operation shouldBe "follow user"
                cause shouldBe dbException
            }
        }
    }

    test("should handle UserNotFoundByUsername exception") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val nonExistentUsername = "non_existent_user"

            coEvery {
                mockUserRepository.follow(validEmail, nonExistentUsername)
            } throws UserException.UserNotFoundByUsername(nonExistentUsername)

            // Act
            val result = useCase(validEmail, nonExistentUsername)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            val failure = result.value
            failure.shouldBeInstanceOf<UserFailure.UserNotFoundByUsername>()
            failure.username shouldBe nonExistentUsername
        }
    }
})
