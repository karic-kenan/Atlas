package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
import io.aethibo.core.utils.Email
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.failure.CommentFailure
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentRepository
import io.aethibo.features.users.domain.model.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime

class CreateCommentUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockCommentRepository = mockk<CommentRepository>()

    val useCase = CreateCommentUseCase { email, slug, comment ->
        createComment(email, slug, comment, mockCommentRepository)
    }

    // Test data
    val validEmail = "user@example.com"
    val validSlug = "test-article"
    val validComment = Comment(
        id = null,
        body = "This is a test comment",
        author = User(
            email = validEmail,
            username = "testuser"
        ),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    val createdComment = validComment.copy(id = 1)

    test("should create comment when all parameters are valid") {
        runTest {
            // Arrange
            coEvery {
                mockCommentRepository.create(validSlug, validEmail, validComment)
            } returns createdComment

            // Act
            val result = useCase(validEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Comment>>()
            result.value.id shouldBe 1
            result.value.body shouldBe "This is a test comment"
        }
    }

    test("should return failure when email is invalid") {
        runTest {
            // Arrange - invalid (blank) email
            val invalidEmail: Email = ""

            // Act
            val result = useCase(invalidEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.InvalidAuthorEmail>()
            ((result).value as CommentFailure.InvalidAuthorEmail).email shouldBe ""
        }
    }

    test("should return failure when email is null") {
        runTest {
            // Arrange - null email
            val nullEmail: Email = null

            // Act
            val result = useCase(nullEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.InvalidAuthorEmail>()
            ((result).value as CommentFailure.InvalidAuthorEmail).email shouldBe ""
        }
    }

    test("should return failure when slug is empty") {
        runTest {
            // Arrange - empty slug
            val emptySlug = ""

            // Act
            val result = useCase(validEmail, emptySlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.InvalidSlug>()
            ((result).value as CommentFailure.InvalidSlug).slug shouldBe ""
        }
    }

    test("should return failure when comment body is empty") {
        runTest {
            // Arrange - comment with empty body
            val emptyComment = validComment.copy(body = "")

            // Act
            val result = useCase(validEmail, validSlug, emptyComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.EmptyCommentBody>()
        }
    }

    test("should return failure when comment creation fails") {
        runTest {
            // Arrange - repository returns null (creation failed)
            coEvery {
                mockCommentRepository.create(validSlug, validEmail, validComment)
            } returns null

            // Act
            val result = useCase(validEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.CommentCreationFailed>()
        }
    }

    test("should handle CommentException from repository") {
        runTest {
            // Arrange - repository throws CommentException
            coEvery {
                mockCommentRepository.create(validSlug, validEmail, validComment)
            } throws CommentException.AuthorNotFound(validEmail)

            // Act
            val result = useCase(validEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.AuthorNotFound>()
        }
    }

    test("should handle generic exceptions and convert to DatabaseError") {
        runTest {
            // Arrange - repository throws generic exception
            val exception = RuntimeException("Database connection failed")
            coEvery {
                mockCommentRepository.create(validSlug, validEmail, validComment)
            } throws exception

            // Act
            val result = useCase(validEmail, validSlug, validComment)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.DatabaseError>()

            val dbError = result.value as CommentFailure.DatabaseError
            dbError.operation shouldBe "comment creation"
            dbError.cause shouldBe exception
        }
    }
})