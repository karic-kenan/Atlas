package io.aethibo.features.comments.domain.usecase

import arrow.core.Either
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

class FindCommentsUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockCommentsRepository = mockk<CommentRepository>()

    val useCase = FindCommentsUseCase { slug ->
        findComments(slug, mockCommentsRepository)
    }

    test("should return comments when slug is valid") {
        runTest {
            // Arrange
            val testSlug = "test-article"
            val testDate = LocalDateTime.now()
            val expectedComments = listOf(
                Comment(
                    id = 1,
                    body = "Test comment 1",
                    createdAt = testDate,
                    updatedAt = testDate,
                    author = User(
                        email = "test@example.com"
                    )
                ),
                Comment(
                    id = 2,
                    body = "Test comment 2",
                    createdAt = testDate,
                    updatedAt = testDate,
                    author = User(
                        email = "another@example.com"
                    )
                )
            )

            coEvery { mockCommentsRepository.find(testSlug) } returns expectedComments

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Comment>>>()
            result.value.size shouldBe 2
            result.value[0].body shouldBe "Test comment 1"
            result.value[1].body shouldBe "Test comment 2"
        }
    }

    test("should return empty list when article has no comments") {
        runTest {
            // Arrange
            val testSlug = "article-without-comments"
            val emptyCommentsList = emptyList<Comment>()

            coEvery { mockCommentsRepository.find(testSlug) } returns emptyCommentsList

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Comment>>>()
            result.value shouldBe emptyList()
        }
    }

    test("should return failure when slug is empty") {
        runTest {
            // Arrange
            val emptySlug = ""

            // Act
            val result = useCase(emptySlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.InvalidSlug>()
            (result.value as CommentFailure.InvalidSlug).slug shouldBe emptySlug
        }
    }

    test("should handle repository exceptions") {
        runTest {
            // Arrange
            val testSlug = "error-slug"
            coEvery { mockCommentsRepository.find(testSlug) } throws
                    CommentException.DatabaseError("test", Exception("DB connection failed"))

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<CommentFailure>>()
            result.value.shouldBeInstanceOf<CommentFailure.DatabaseError>()
        }
    }
})
