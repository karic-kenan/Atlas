package io.aethibo.features.users.domain.usecase

import arrow.core.Either
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.failure.UserFailure
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class UpdateUserUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockUsersRepository = mockk<UsersRepository>()

    val useCase = UpdateUserUseCase { email, user ->
        updateUser(mockUsersRepository, email, user)
    }

    test("should update user successfully") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val updateRequest = User(
                email = "updated@example.com",
                username = "updatedUsername",
                bio = "Updated bio",
                image = "updated-image.jpg",
                password = "newpassword123"
            )

            val updatedUser = updateRequest.copy()

            coEvery { mockUsersRepository.update(validEmail, updateRequest) } returns updatedUser

            // Act
            val result = useCase(validEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Right<User>>()
            val resultUser = result.value
            resultUser.email shouldBe updateRequest.email
            resultUser.username shouldBe updateRequest.username
            resultUser.bio shouldBe updateRequest.bio
            resultUser.image shouldBe updateRequest.image
            resultUser.password shouldBe null // Password should be nullified in response
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val updateRequest = User(
                email = "updated@example.com",
                username = "updatedUsername"
            )

            // Act
            val result = useCase(emptyEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            (result.value as UserFailure.InvalidEmail).email shouldBe emptyEmail
        }
    }

    test("should return failure when email is blank") {
        runTest {
            // Arrange
            val blankEmail = "   "
            val updateRequest = User(
                email = "updated@example.com",
                username = "updatedUsername"
            )

            // Act
            val result = useCase(blankEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            (result.value as UserFailure.InvalidEmail).email shouldBe blankEmail
        }
    }

    test("should return failure when user update fails") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val updateRequest = User(
                email = "updated@example.com",
                username = "updatedUsername"
            )

            coEvery { mockUsersRepository.update(validEmail, updateRequest) } returns null

            // Act
            val result = useCase(validEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.UserUpdateFailed>()
            (result.value as UserFailure.UserUpdateFailed).email shouldBe validEmail
        }
    }

    test("should handle UserException from repository") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val updateRequest = User(
                email = "invalid@",
                username = "updatedUsername"
            )

            coEvery {
                mockUsersRepository.update(validEmail, updateRequest)
            } throws UserException.InvalidEmail("invalid@")

            // Act
            val result = useCase(validEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.InvalidEmail>()
            (result.value as UserFailure.InvalidEmail).email shouldBe "invalid@"
        }
    }

    test("should handle generic exceptions from repository") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val updateRequest = User(
                email = "updated@example.com",
                username = "updatedUsername"
            )

            val exception = RuntimeException("Database connection failed")
            coEvery {
                mockUsersRepository.update(validEmail, updateRequest)
            } throws exception

            // Act
            val result = useCase(validEmail, updateRequest)

            // Assert
            result.shouldBeInstanceOf<Either.Left<UserFailure>>()
            result.value.shouldBeInstanceOf<UserFailure.DatabaseError>()
            val dbError = result.value as UserFailure.DatabaseError
            dbError.operation shouldBe "user update"
            dbError.cause shouldBe exception
        }
    }
})