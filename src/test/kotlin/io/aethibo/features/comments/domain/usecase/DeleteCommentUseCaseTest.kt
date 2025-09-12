package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
import io.aethibo.core.utils.CommentId
import io.aethibo.core.utils.Slug
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.failure.CommentFailure
import io.aethibo.features.comments.domain.repository.CommentRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class DeleteCommentUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockCommentRepository = mockk<CommentRepository>()

    val useCase = DeleteCommentUseCase { commentId, slug ->
        deleteComment(commentId, slug, mockCommentRepository)
    }

    test("should successfully delete comment when parameters are valid") {
        runTest {
            // Arrange
            val validCommentId: CommentId = 123
            val validSlug: Slug = "test-article"

            // Mock the repository call
            coJustRun { mockCommentRepository.delete(validCommentId, validSlug) }

            // Act
            val result = useCase(validCommentId, validSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Unit>>()
            coVerify(exactly = 1) { mockCommentRepository.delete(validCommentId, validSlug) }
        }
    }

    test("should handle CommentException from repository") {
        runTest {
            // Arrange
            val validCommentId: CommentId = 123
            val validSlug: Slug = "test-article"
            val exception = CommentException.CommentNotFound(validCommentId)

            coEvery { mockCommentRepository.delete(validCommentId, validSlug) } throws exception

            // Act
            val result = useCase(validCommentId, validSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.CommentNotFound>()
        }
    }

    test("should handle generic exceptions from repository") {
        runTest {
            // Arrange
            val validCommentId: CommentId = 123
            val validSlug: Slug = "test-article"
            val genericException = RuntimeException("Database connection failed")

            coEvery { mockCommentRepository.delete(validCommentId, validSlug) } throws genericException

            // Act
            val result = useCase(validCommentId, validSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.DatabaseError>()
            // Additional checks if needed for the error message
            (result.value as CommentFailure.DatabaseError).operation shouldBe "comment deletion"
            (result.value as CommentFailure.DatabaseError).cause shouldBe genericException
        }
    }
})
