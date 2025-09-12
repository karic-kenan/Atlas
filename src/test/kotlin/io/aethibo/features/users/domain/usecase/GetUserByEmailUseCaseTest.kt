package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.TokenPair
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class GetUserByEmailUseCaseTest : FunSpec({
    // Mock dependencies
    val mockUsersRepository = mockk<UsersRepository>()
    val mockJwtProvider = mockk<JwtProvider>()

    val useCase = GetUserByEmailUseCase { email ->
        getUserByEmail(mockUsersRepository, mockJwtProvider, email)
    }

    test("should return user when valid email exists and user is active") {
        runTest {
            // Arrange
            val testEmail = "test@example.com"
            val testUser = User(
                id = 1,
                email = testEmail,
                username = "testuser",
                password = "hashedpassword",
                isActive = true
            )

            val testTokenPair = TokenPair(
                accessToken = "fake-access-token",
                refreshToken = "fake-refresh-token",
                expiresIn = 1000L
            )

            coEvery { mockUsersRepository.findByEmail(testEmail) } returns testUser
            every { mockJwtProvider.createTokenPair(testUser, any()) } returns testTokenPair

            // Act
            val result = useCase(testEmail)

            // Assert
            result.shouldBeInstanceOf<Either.Right<User>>()
            val userResult = result.value
            userResult.email shouldBe testEmail
            userResult.token shouldBe testTokenPair.accessToken
            userResult.refreshToken shouldBe testTokenPair.refreshToken
            userResult.password shouldBe null // Password should be null in response
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""

            // Act
            val result = useCase(emptyEmail)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
        }
    }

    test("should return failure when user not found") {
        runTest {
            // Arrange
            val nonExistentEmail = "nonexistent@example.com"
            coEvery { mockUsersRepository.findByEmail(nonExistentEmail) } returns null

            // Act
            val result = useCase(nonExistentEmail)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserNotFoundByEmail>()
        }
    }

    test("should return failure when user is inactive") {
        runTest {
            // Arrange
            val inactiveUserEmail = "inactive@example.com"
            val inactiveUser = User(
                id = 2,
                email = inactiveUserEmail,
                username = "inactive",
                password = "hashedpassword",
                isActive = false
            )

            coEvery { mockUsersRepository.findByEmail(inactiveUserEmail) } returns inactiveUser

            // Act
            val result = useCase(inactiveUserEmail)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserInactive>()
        }
    }

    test("should handle repository exceptions") {
        runTest {
            // Arrange
            val errorEmail = "error@example.com"
            coEvery { mockUsersRepository.findByEmail(errorEmail) } throws
                    UserException.DatabaseError("test", Exception("DB connection failed"))

            // Act
            val result = useCase(errorEmail)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.DatabaseError>()
        }
    }
})
