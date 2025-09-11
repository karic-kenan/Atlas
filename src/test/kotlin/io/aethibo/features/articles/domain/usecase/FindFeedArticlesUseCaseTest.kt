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

class FindFeedUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()

    val useCase = FindFeedUseCase { email, limit, offset ->
        findFeed(
            articleRepository = mockArticleRepository,
            email = email,
            limit = limit,
            offset = offset
        )
    }

    test("should return feed articles when email is valid") {
        runTest {
            // Arrange
            val testEmail = "user@example.com"
            val limit = 20
            val offset = 0L
            val expectedArticles = listOf(
                Article(
                    slug = "test-article-1",
                    title = "Test Article 1",
                    description = "Description 1",
                    body = "Body content 1",
                    tagList = listOf("test"),
                    createdAt = null,
                    updatedAt = null
                ),
                Article(
                    slug = "test-article-2",
                    title = "Test Article 2",
                    description = "Description 2",
                    body = "Body content 2",
                    tagList = listOf("test"),
                    createdAt = null,
                    updatedAt = null
                )
            )

            coEvery { mockArticleRepository.getFeed(testEmail, limit, offset) } returns expectedArticles

            // Act
            val result = useCase(testEmail, limit, offset)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Article>>>()
            result.value.size shouldBe 2
            result.value[0].slug shouldBe "test-article-1"
            result.value[1].slug shouldBe "test-article-2"
        }
    }

    test("should return failure when email is invalid") {
        runTest {
            // Arrange
            val invalidEmail = ""
            val limit = 20
            val offset = 0L

            // Act
            val result = useCase(invalidEmail, limit, offset)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidAuthorEmail>()
        }
    }

    test("should return failure when no followed authors exist") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val limit = 20
            val offset = 0L

            coEvery { mockArticleRepository.getFeed(validEmail, limit, offset) } throws
                    ArticleException.NoFollowedAuthors

            // Act
            val result = useCase(validEmail, limit, offset)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.NoFollowedAuthors>()
        }
    }

    test("should return failure when limit is invalid") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val invalidLimit = -1
            val offset = 0L

            coEvery { mockArticleRepository.getFeed(validEmail, invalidLimit, offset) } throws
                    ArticleException.InvalidLimit(invalidLimit)

            // Act
            val result = useCase(validEmail, invalidLimit, offset)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidLimit>()
        }
    }

    test("should return failure when offset is invalid") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val limit = 20
            val invalidOffset = -1L

            coEvery { mockArticleRepository.getFeed(validEmail, limit, invalidOffset) } throws
                    ArticleException.InvalidOffset(invalidOffset)

            // Act
            val result = useCase(validEmail, limit, invalidOffset)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidOffset>()
        }
    }

    test("should handle general repository exceptions") {
        runTest {
            // Arrange
            val validEmail = "user@example.com"
            val limit = 20
            val offset = 0L

            coEvery { mockArticleRepository.getFeed(validEmail, limit, offset) } throws
                    Exception("Unexpected database error")

            // Act
            val result = useCase(validEmail, limit, offset)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            (result as Either.Left<ArticleFailure>).value.shouldBeInstanceOf<ArticleFailure.DatabaseError>()
        }
    }
})