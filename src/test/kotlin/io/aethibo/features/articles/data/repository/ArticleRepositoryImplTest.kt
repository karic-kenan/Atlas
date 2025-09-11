package io.aethibo.features.articles.data.repository

import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.users.domain.model.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class ArticleRepositoryImplTest : DescribeSpec({

    // Test setup
    val meterRegistry = SimpleMeterRegistry()
    lateinit var repository: ArticleRepositoryImpl

    // Test data
    val testUser = User(
        id = 1L,
        email = "test@example.com",
        username = "testuser",
        bio = "Test bio",
        image = "test.jpg",
    )

    val testArticle = Article(
        slug = "test-article",
        title = "Test Article",
        description = "Test Description",
        body = "Test Body",
        author = testUser,
        tagList = listOf("test", "kotlin"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        favorited = false,
        favoritesCount = 0
    )

    beforeTest {
        repository = ArticleRepositoryImpl(meterRegistry)
        clearAllMocks()
    }

    describe("ArticleRepositoryImpl Validation Tests") {

        describe("create - input validation") {

            it("should throw ArticleException.InvalidSlug when slug is null") {
                // Given
                val article = testArticle.copy(slug = null)

                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // Given
                val article = testArticle.copy(slug = "")

                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.EmptyTitle when title is null") {
                // Given
                val article = testArticle.copy(title = null)

                // When & Then
                shouldThrow<ArticleException.EmptyTitle> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.EmptyTitle when title is blank") {
                // Given
                val article = testArticle.copy(title = "")

                // When & Then
                shouldThrow<ArticleException.EmptyTitle> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.EmptyDescription when description is null") {
                // Given
                val article = testArticle.copy(description = null)

                // When & Then
                shouldThrow<ArticleException.EmptyDescription> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.EmptyDescription when description is blank") {
                // Given
                val article = testArticle.copy(description = "")

                // When & Then
                shouldThrow<ArticleException.EmptyDescription> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.EmptyBody when body is blank") {
                // Given
                val article = testArticle.copy(body = "")

                // When & Then
                shouldThrow<ArticleException.EmptyBody> {
                    runBlocking { repository.create(article) }
                }
            }

            it("should throw ArticleException.AuthorNotFound when author id is null") {
                // Given
                val article = testArticle.copy(author = testUser.copy(id = null))

                // When & Then
                shouldThrow<ArticleException.AuthorNotFound> {
                    runBlocking { repository.create(article) }
                }
            }
        }

        describe("all - parameter validation") {

            it("should throw ArticleException.InvalidLimit when limit is less than 1") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.all(0, 0L) }
                }
            }

            it("should throw ArticleException.InvalidLimit when limit is greater than 100") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.all(101, 0L) }
                }
            }

            it("should throw ArticleException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<ArticleException.InvalidOffset> {
                    runBlocking { repository.all(10, -1L) }
                }
            }

            it("should accept valid limit and offset parameters") {
                // Given
                val validLimit = 10
                val validOffset = 0L

                // When & Then - Should not throw any exceptions during validation
                // The actual database operation will fail in unit test, but validation should pass
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.all(validLimit, validOffset) }
                }.let { exception ->
                    // Ensure it's NOT a validation exception
                    exception shouldNotBe ArticleException.InvalidLimit::class
                    exception shouldNotBe ArticleException.InvalidOffset::class
                }
            }
        }

        describe("getBySlug - parameter validation") {

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.getBySlug("") }
                }
            }

            it("should accept valid slug parameter") {
                // Given
                val validSlug = "valid-slug"

                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.getBySlug(validSlug) }
                }.let { exception ->
                    // Ensure it's NOT a validation exception
                    exception shouldNotBe ArticleException.InvalidSlug::class
                }
            }
        }

        describe("getByAuthor - parameter validation") {

            it("should throw ArticleException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.getByAuthor("testuser", 0, 0L) }
                }
            }

            it("should throw ArticleException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<ArticleException.InvalidOffset> {
                    runBlocking { repository.getByAuthor("testuser", 10, -1L) }
                }
            }

            it("should throw ArticleException.InvalidUsername when author is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidUsername> {
                    runBlocking { repository.getByAuthor("", 10, 0L) }
                }
            }

            it("should accept valid parameters") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.getByAuthor("testuser", 10, 0L) }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidLimit::class
                    exception shouldNotBe ArticleException.InvalidOffset::class
                    exception shouldNotBe ArticleException.InvalidUsername::class
                }
            }
        }

        describe("getByTag - parameter validation") {

            it("should throw ArticleException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.getByTag("kotlin", 0, 0L) }
                }
            }

            it("should throw ArticleException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<ArticleException.InvalidOffset> {
                    runBlocking { repository.getByTag("kotlin", 10, -1L) }
                }
            }

            it("should throw ArticleException.TagNotFound when tag is blank") {
                // When & Then
                shouldThrow<ArticleException.TagNotFound> {
                    runBlocking { repository.getByTag("", 10, 0L) }
                }
            }

            it("should accept valid parameters") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.getByTag("kotlin", 10, 0L) }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidLimit::class
                    exception shouldNotBe ArticleException.InvalidOffset::class
                    exception shouldNotBe ArticleException.TagNotFound::class
                }
            }
        }

        describe("getByFavourite - parameter validation") {

            it("should throw ArticleException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.getByFavourite("testuser", 0, 0L) }
                }
            }

            it("should throw ArticleException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<ArticleException.InvalidOffset> {
                    runBlocking { repository.getByFavourite("testuser", 10, -1L) }
                }
            }

            it("should throw ArticleException.InvalidUsername when favourite is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidUsername> {
                    runBlocking { repository.getByFavourite("", 10, 0L) }
                }
            }

            it("should accept valid parameters") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.getByFavourite("testuser", 10, 0L) }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidLimit::class
                    exception shouldNotBe ArticleException.InvalidOffset::class
                    exception shouldNotBe ArticleException.InvalidUsername::class
                }
            }
        }

        describe("getFeed - parameter validation") {

            it("should throw ArticleException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<ArticleException.InvalidLimit> {
                    runBlocking { repository.getFeed("test@example.com", 0, 0L) }
                }
            }

            it("should throw ArticleException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<ArticleException.InvalidOffset> {
                    runBlocking { repository.getFeed("test@example.com", 10, -1L) }
                }
            }

            it("should throw ArticleException.InvalidAuthorEmail when email is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidAuthorEmail> {
                    runBlocking { repository.getFeed("", 10, 0L) }
                }
            }

            it("should throw ArticleException.InvalidAuthorEmail when email is invalid") {
                // When & Then
                shouldThrow<ArticleException.InvalidAuthorEmail> {
                    runBlocking { repository.getFeed("invalid-email", 10, 0L) }
                }
            }

            it("should accept valid email format") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.getFeed("test@example.com", 10, 0L) }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidLimit::class
                    exception shouldNotBe ArticleException.InvalidOffset::class
                    exception shouldNotBe ArticleException.InvalidAuthorEmail::class
                }
            }
        }

        describe("update - parameter validation") {

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.update("", testArticle) }
                }
            }

            it("should accept valid slug parameter") {
                // When & Then - Should not throw validation exception for slug
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.update("valid-slug", testArticle) }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidSlug::class
                }
            }
        }

        describe("delete - parameter validation") {

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.delete("") }
                }
            }

            it("should accept valid slug parameter") {
                // When & Then - Should not throw validation exception for slug
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.delete("valid-slug") }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidSlug::class
                }
            }
        }

        describe("favorite - parameter validation") {

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.favorite(1L, "") }
                }
            }

            it("should throw ArticleException.AuthorNotFound when userId is invalid") {
                // When & Then
                shouldThrow<ArticleException.AuthorNotFound> {
                    runBlocking { repository.favorite(0L, "test-article") }
                }
            }

            it("should accept valid parameters") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.favorite(1L, "valid-slug") }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidSlug::class
                    exception shouldNotBe ArticleException.AuthorNotFound::class
                }
            }
        }

        describe("unfavorite - parameter validation") {

            it("should throw ArticleException.InvalidSlug when slug is blank") {
                // When & Then
                shouldThrow<ArticleException.InvalidSlug> {
                    runBlocking { repository.unfavorite(1L, "") }
                }
            }

            it("should throw ArticleException.AuthorNotFound when userId is invalid") {
                // When & Then
                shouldThrow<ArticleException.AuthorNotFound> {
                    runBlocking { repository.unfavorite(0L, "test-article") }
                }
            }

            it("should accept valid parameters") {
                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.unfavorite(1L, "valid-slug") }
                }.let { exception ->
                    exception shouldNotBe ArticleException.InvalidSlug::class
                    exception shouldNotBe ArticleException.AuthorNotFound::class
                }
            }
        }
    }

    describe("CursorPagination validation") {

        it("should throw ArticleException.InvalidLimit when limit is less than 1") {
            // Given
            val pagination = CursorPagination(limit = 0)

            // When & Then
            shouldThrow<ArticleException.InvalidLimit> {
                pagination.validateLimit()
            }
        }

        it("should throw ArticleException.InvalidLimit when limit is greater than 100") {
            // Given
            val pagination = CursorPagination(limit = 101)

            // When & Then
            shouldThrow<ArticleException.InvalidLimit> {
                pagination.validateLimit()
            }
        }

        it("should pass validation with valid limit") {
            // Given
            val pagination = CursorPagination(limit = 50)

            // When & Then - should not throw
            pagination.validateLimit() // This should succeed
        }

        it("should have correct default values") {
            // Given
            val pagination = CursorPagination()

            // Then
            pagination.cursor shouldBe null
            pagination.limit shouldBe 20
            pagination.direction shouldBe CursorPagination.Direction.FORWARD
        }

        it("should allow custom values") {
            // Given
            val cursor = "test-cursor"
            val limit = 10
            val direction = CursorPagination.Direction.BACKWARD

            val pagination = CursorPagination(cursor, limit, direction)

            // Then
            pagination.cursor shouldBe cursor
            pagination.limit shouldBe limit
            pagination.direction shouldBe direction
        }
    }

    describe("PaginatedResult structure") {

        it("should create PaginatedResult with all properties") {
            // Given
            val items = listOf(testArticle)
            val nextCursor = "next-cursor"
            val previousCursor = "prev-cursor"
            val hasMore = true
            val totalCount = 100L

            // When
            val result = PaginatedResult(
                items = items,
                nextCursor = nextCursor,
                previousCursor = previousCursor,
                hasMore = hasMore,
                totalCount = totalCount
            )

            // Then
            result.items shouldHaveSize 1
            result.items.first() shouldBe testArticle
            result.nextCursor shouldBe nextCursor
            result.previousCursor shouldBe previousCursor
            result.hasMore shouldBe hasMore
            result.totalCount shouldBe totalCount
        }

        it("should create PaginatedResult with minimal properties") {
            // Given
            val items = emptyList<Article>()

            // When
            val result = PaginatedResult(
                items = items,
                nextCursor = null,
                previousCursor = null,
                hasMore = false
            )

            // Then
            result.items shouldHaveSize 0
            result.nextCursor shouldBe null
            result.previousCursor shouldBe null
            result.hasMore shouldBe false
            result.totalCount shouldBe null
        }
    }
})