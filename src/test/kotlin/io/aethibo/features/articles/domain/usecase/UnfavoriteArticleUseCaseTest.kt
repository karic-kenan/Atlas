package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class UnfavoriteArticleUseCaseTest : FunSpec({
    // Setup - mock dependencies
    val mockArticleRepository = mockk<ArticleRepository>()
    val mockUserRepository = mockk<UsersRepository>()

    val useCase = UnfavoriteArticleUseCase { email, slug ->
        unfavoriteArticle(mockUserRepository, mockArticleRepository, email, slug)
    }

    test("should unfavorite article successfully") {
        runTest {
            // Arrange
            val testEmail = "user@example.com"
            val testSlug = "test-article"
            val testUserId = 1L

            val testUser = User(
                id = testUserId,
                email = testEmail,
                username = "testuser",
                password = "hashedpassword",
                bio = null,
                image = null
            )

            val article = Article(
                slug = testSlug,
                title = "Test Article",
                description = "Description",
                body = "Body content",
                tagList = listOf("test"),
                favorited = true,
                favoritesCount = 1,
                createdAt = null,
                updatedAt = null
            )

            val unfavoritedArticle = article.copy(
                favorited = false,
                favoritesCount = 0
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns article
            coEvery { mockUserRepository.findByEmail(testEmail) } returns testUser
            coEvery { mockArticleRepository.unfavorite(testUserId, testSlug) } returns 1
            coEvery { mockArticleRepository.getBySlug(testSlug) } returns unfavoritedArticle

            // Act
            val result = useCase(testEmail, testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Article>>()
            val returnedArticle = result.value
            returnedArticle.favorited shouldBe false
            returnedArticle.favoritesCount shouldBe 1L
        }
    }

    test("should return InvalidAuthorEmail when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val testSlug = "test-article"

            // Act
            val result = useCase(emptyEmail, testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidAuthorEmail>()
        }
    }

    test("should return ArticleNotFound when slug doesn't exist") {
        runTest {
            // Arrange
            val testEmail = "user@example.com"
            val nonExistentSlug = "non-existent"

            coEvery { mockArticleRepository.getBySlug(nonExistentSlug) } returns null

            // Act
            val result = useCase(testEmail, nonExistentSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleNotFound>()
        }
    }

    test("should return UserNotFound when user doesn't exist") {
        runTest {
            // Arrange
            val nonExistentEmail = "nonexistent@example.com"
            val testSlug = "test-article"
            val article = Article(
                slug = testSlug,
                title = "Test Article",
                description = "Description",
                body = "Body content",
                tagList = listOf("test"),
                createdAt = null,
                updatedAt = null
            )

            coEvery { mockArticleRepository.getBySlug(testSlug) } returns article
            coEvery { mockUserRepository.findByEmail(nonExistentEmail) } returns null

            // Act
            val result = useCase(nonExistentEmail, testSlug)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.UserNotFound>()
        }
    }
})