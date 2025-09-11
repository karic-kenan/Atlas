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

class UpdateArticleUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()

    val useCase = UpdateArticleUseCase { slug, article ->
        updateArticle(mockArticleRepository, slug, article)
    }

    test("should update article successfully when slug and article are valid") {
        runTest {
            // Arrange
            val testSlug = "test-article"
            val existingArticle = Article(
                slug = testSlug,
                title = "Original Title",
                description = "Original Description",
                body = "Original Body",
                tagList = listOf("original"),
                createdAt = null,
                updatedAt = null
            )

            val updatedArticle = Article(
                slug = testSlug,
                title = "Updated Title",
                description = "Updated Description",
                body = "Updated Body",
                tagList = listOf("updated"),
                createdAt = null,
                updatedAt = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns existingArticle
            coEvery { mockArticleRepository.update(testSlug, any()) } returns updatedArticle

            // Act
            val result = useCase(testSlug, updatedArticle)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Article>>()
            result.value shouldBe updatedArticle
        }
    }

    test("should return ArticleNotFound when article doesn't exist") {
        runTest {
            // Arrange
            val testSlug = "non-existent"
            val articleToUpdate = Article(
                slug = testSlug,
                title = "Updated Title",
                description = "Updated Description",
                body = "Updated Body",
                tagList = listOf("updated"),
                createdAt = null,
                updatedAt = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns null

            // Act
            val result = useCase(testSlug, articleToUpdate)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleNotFound>()
        }
    }

    test("should return ArticleUpdateFailed when update returns null") {
        runTest {
            // Arrange
            val testSlug = "test-article"
            val existingArticle = Article(
                slug = testSlug,
                title = "Original Title",
                description = "Original Description",
                body = "Original Body",
                tagList = listOf("original"),
                createdAt = null,
                updatedAt = null
            )

            val updatedArticle = Article(
                slug = testSlug,
                title = "Updated Title",
                description = "Updated Description",
                body = "Updated Body",
                tagList = listOf("updated"),
                createdAt = null,
                updatedAt = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns existingArticle
            coEvery { mockArticleRepository.update(testSlug, any()) } returns null

            // Act
            val result = useCase(testSlug, updatedArticle)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleUpdateFailed>()
        }
    }

    test("should handle repository exceptions") {
        runTest {
            // Arrange
            val testSlug = "error-slug"
            val articleToUpdate = Article(
                slug = testSlug,
                title = "Updated Title",
                description = "Updated Description",
                body = "Updated Body",
                tagList = listOf("updated"),
                createdAt = null,
                updatedAt = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } throws
                    ArticleException.DatabaseError("lookup", Exception("DB connection failed"))

            // Act
            val result = useCase(testSlug, articleToUpdate)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.DatabaseError>()
        }
    }
})
