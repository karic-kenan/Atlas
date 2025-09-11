package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class GetArticleBySlugUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()

    val useCase = GetArticleBySlugUseCase { slug ->
        getArticleBySlug(mockArticleRepository, slug)
    }

    test("should return article when slug is valid") {
        runTest {
            // Arrange
            val testSlug = "test-article"
            val expectedArticle = Article(
                slug = testSlug,
                title = "Test Article",
                description = "Description",
                body = "Body content",
                tagList = listOf("test"),
                createdAt = null,
                updatedAt = null,
                author = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns expectedArticle

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Article>>()
            result.value.slug shouldBe testSlug
        }
    }

    test("should return failure when slug is empty") {
        runTest {
            // Arrange
            val emptySlug = ""

            // Act
            val result = useCase(emptySlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidSlug>()
        }
    }

    test("should return failure when article not found") {
        runTest {
            // Arrange
            val nonExistentSlug = "non-existent"
            coEvery { mockArticleRepository.getBySlug(nonExistentSlug) } returns null

            // Act
            val result = useCase(nonExistentSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleNotFound>()
        }
    }

    test("should handle repository exceptions") {
        runTest {
            // Arrange
            val testSlug = "error-slug"
            coEvery { mockArticleRepository.getBySlug(testSlug) } throws
                    ArticleException.DatabaseError("test", Exception("DB connection failed"))

            // Act
            val result = useCase(testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.DatabaseError>()
        }
    }
})
