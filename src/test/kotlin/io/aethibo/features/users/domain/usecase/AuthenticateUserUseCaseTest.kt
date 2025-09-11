package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.SecureArgon2Cipher
import io.aethibo.core.security.TokenPair
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.test.runTest

class AuthenticateUserUseCaseTest : FunSpec({
    // Mock dependencies
    val mockUserRepository = mockk<UsersRepository>()
    val mockJwtProvider = mockk<JwtProvider>()

    // Setup mocks for SecureArgon2Cipher static methods
    mockkObject(SecureArgon2Cipher)

    val useCase = AuthenticateUserUseCase { user ->
        authenticateUser(mockUserRepository, mockJwtProvider, user)
    }

    afterSpec {
        unmockkAll() // Clean up mocks after tests
    }

    test("should successfully authenticate user with valid credentials") {
        runTest {
            // Arrange
            val email = "test@example.com"
            val password = "password123"
            val hashedPassword = "hashed_password"

            val loginUser = User(
                email = email,
                password = password
            )

            val foundUser = User(
                id = 1,
                email = email,
                username = "testuser",
                password = hashedPassword,
                isActive = true
            )

            val tokenPair = TokenPair(
                accessToken = "access_token",
                refreshToken = "refresh_token",
                expiresIn = 1000L
            )

            // Mock repository response
            coEvery { mockUserRepository.findByEmail(email) } returns foundUser

            // Mock password verification
            every { SecureArgon2Cipher.verifyPassword(password, hashedPassword) } returns true

            // Mock token generation
            every { mockJwtProvider.createTokenPair(foundUser, any()) } returns tokenPair

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Right<User>>()
            val authenticatedUser = result.value

            authenticatedUser.email shouldBe email
            authenticatedUser.token shouldBe tokenPair.accessToken
            authenticatedUser.refreshToken shouldBe tokenPair.refreshToken
            authenticatedUser.password shouldBe null // Password should be nullified
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val loginUser = User(
                email = "",
                password = "password123"
            )

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
        }
    }

    test("should return failure when password is empty") {
        runTest {
            // Arrange
            val loginUser = User(
                email = "test@example.com",
                password = ""
            )

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.EmptyRequiredField>()
        }
    }

    test("should return failure when user is not found") {
        runTest {
            // Arrange
            val email = "nonexistent@example.com"
            val loginUser = User(
                email = email,
                password = "password123"
            )

            // Mock repository response
            coEvery { mockUserRepository.findByEmail(email) } returns null

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserNotFoundByEmail>()
        }
    }

    test("should return failure when user account is inactive") {
        runTest {
            // Arrange
            val email = "inactive@example.com"
            val password = "password123"

            val loginUser = User(
                email = email,
                password = password
            )

            val inactiveUser = User(
                id = 2,
                email = email,
                username = "inactiveuser",
                password = "hashed_password",
                isActive = false
            )

            // Mock repository response
            coEvery { mockUserRepository.findByEmail(email) } returns inactiveUser

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserInactive>()
        }
    }

    test("should return failure when password is incorrect") {
        runTest {
            // Arrange
            val email = "test@example.com"
            val password = "wrong_password"
            val hashedPassword = "hashed_password"

            val loginUser = User(
                email = email,
                password = password
            )

            val foundUser = User(
                id = 1,
                email = email,
                username = "testuser",
                password = hashedPassword,
                isActive = true
            )

            // Mock repository response
            coEvery { mockUserRepository.findByEmail(email) } returns foundUser

            // Mock password verification - returns false for wrong password
            every { SecureArgon2Cipher.verifyPassword(password, hashedPassword) } returns false

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidPassword>()
        }
    }

    test("should handle database exceptions") {
        runTest {
            // Arrange
            val email = "test@example.com"
            val loginUser = User(
                email = email,
                password = "password123"
            )

            // Mock repository to throw exception
            coEvery { mockUserRepository.findByEmail(email) } throws
                    UserException.DatabaseError("test", Exception("DB connection failed"))

            // Act
            val result = useCase(loginUser)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.DatabaseError>()
        }
    }
})