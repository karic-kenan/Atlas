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

class FindArticlesUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()

    val useCase = FindArticlesUseCase { limit, offset, tag, author, favourite ->
        findArticles(
            articleRepository = mockArticleRepository,
            limit = limit,
            offset = offset,
            tag = tag,
            author = author,
            favourite = favourite
        )
    }

    test("should return all articles when no filters are specified") {
        runTest {
            // Arrange
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

            coEvery { mockArticleRepository.all(limit, offset) } returns expectedArticles

            // Act
            val result = useCase(limit, offset, null, null, null)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Article>>>()
            result.value.size shouldBe 2
            result.value[0].slug shouldBe "test-article-1"
            result.value[1].slug shouldBe "test-article-2"
        }
    }

    test("should return articles filtered by tag") {
        runTest {
            // Arrange
            val limit = 20
            val offset = 0L
            val tag = "programming"
            val expectedArticles = listOf(
                Article(
                    slug = "programming-article",
                    title = "Programming Article",
                    description = "Programming Description",
                    body = "Programming content",
                    tagList = listOf("programming"),
                    createdAt = null,
                    updatedAt = null
                )
            )

            coEvery { mockArticleRepository.getByTag(tag, limit, offset) } returns expectedArticles

            // Act
            val result = useCase(limit, offset, tag, null, null)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Article>>>()
            result.value.size shouldBe 1
            result.value[0].slug shouldBe "programming-article"
            result.value[0].tagList shouldBe listOf("programming")
        }
    }

    test("should return articles filtered by author") {
        runTest {
            // Arrange
            val limit = 20
            val offset = 0L
            val author = "john"
            val expectedArticles = listOf(
                Article(
                    slug = "johns-article",
                    title = "John's Article",
                    description = "John's Description",
                    body = "John's content",
                    tagList = listOf("john"),
                    createdAt = null,
                    updatedAt = null
                )
            )

            coEvery { mockArticleRepository.getByAuthor(author, limit, offset) } returns expectedArticles

            // Act
            val result = useCase(limit, offset, null, author, null)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Article>>>()
            result.value.size shouldBe 1
            result.value[0].slug shouldBe "johns-article"
        }
    }

    test("should return articles filtered by favourite") {
        runTest {
            // Arrange
            val limit = 20
            val offset = 0L
            val favourite = "jane"
            val expectedArticles = listOf(
                Article(
                    slug = "janes-favorite",
                    title = "Jane's Favorite",
                    description = "Jane's Favorite Description",
                    body = "Jane's favorite content",
                    tagList = listOf("favorite"),
                    createdAt = null,
                    updatedAt = null
                )
            )

            coEvery { mockArticleRepository.getByFavourite(favourite, limit, offset) } returns expectedArticles

            // Act
            val result = useCase(limit, offset, null, null, favourite)

            // Assert
            result.shouldBeInstanceOf<Either.Right<List<Article>>>()
            result.value.size shouldBe 1
            result.value[0].slug shouldBe "janes-favorite"
        }
    }

    test("should return failure when limit is invalid") {
        runTest {
            // Arrange
            val invalidLimit = -1
            val offset = 0L

            coEvery { mockArticleRepository.all(invalidLimit, offset) } throws
                    ArticleException.InvalidLimit(invalidLimit)

            // Act
            val result = useCase(invalidLimit, offset, null, null, null)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidLimit>()
        }
    }

    test("should return failure when offset is invalid") {
        runTest {
            // Arrange
            val limit = 20
            val invalidOffset = -1L

            coEvery { mockArticleRepository.all(limit, invalidOffset) } throws
                    ArticleException.InvalidOffset(invalidOffset)

            // Act
            val result = useCase(limit, invalidOffset, null, null, null)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidOffset>()
        }
    }

    test("should handle general repository exceptions") {
        runTest {
            // Arrange
            val limit = 20
            val offset = 0L

            coEvery { mockArticleRepository.all(limit, offset) } throws
                    Exception("Unexpected database error")

            // Act
            val result = useCase(limit, offset, null, null, null)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.DatabaseError>()
        }
    }
})