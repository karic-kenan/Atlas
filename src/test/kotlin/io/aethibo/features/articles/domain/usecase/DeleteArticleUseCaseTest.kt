package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class DeleteArticleUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()

    val useCase = DeleteArticleUseCase { slug ->
        deleteArticle(mockArticleRepository, slug)
    }

    test("should delete article successfully when slug is valid") {
        runTest {
            // Arrange
            val testSlug = "test-article"
            coEvery { mockArticleRepository.delete(testSlug) } returns Unit

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Unit>>()
            coVerify(exactly = 1) { mockArticleRepository.delete(testSlug) }
        }
    }

    test("should return failure when repository throws ArticleException") {
        runTest {
            // Arrange
            val testSlug = "non-existent"
            coEvery { mockArticleRepository.delete(testSlug) } throws
                    ArticleException.ArticleNotFound(testSlug)

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleNotFound>()
        }
    }

    test("should return DatabaseError failure on generic exception") {
        runTest {
            // Arrange
            val testSlug = "error-slug"
            coEvery { mockArticleRepository.delete(testSlug) } throws
                    RuntimeException("Database connection failed")

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.DatabaseError>()
        }
    }
})
