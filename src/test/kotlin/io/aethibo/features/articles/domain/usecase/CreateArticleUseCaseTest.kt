package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import com.github.slugify.Slugify
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime

class CreateArticleUseCaseTest : FunSpec({
    // Mock dependencies
    val mockSlugifyBuilder = mockk<Slugify>()
    val mockUserRepository = mockk<UsersRepository>()
    val mockArticleRepository = mockk<ArticleRepository>()

    // Create the use case with mocked dependencies
    val useCase = CreateArticleUseCase { email, articleToCreate ->
        createArticle(mockUserRepository, mockArticleRepository, mockSlugifyBuilder, email, articleToCreate)
    }

    test("should create article successfully when inputs are valid") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val articleToCreate = Article(
                title = "Test Article",
                description = "Test description",
                body = "Test body content",
                tagList = listOf("test", "article")
            )

            val mockUser = mockk<User> {
                every { id } returns 1L
            }

            val slugifiedTitle = "test-article"

            val createdArticle = articleToCreate.copy(
                slug = slugifiedTitle,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                author = mockUser
            )

            // Setup all necessary mocks
            coEvery { mockUserRepository.findByEmail(validEmail) } returns mockUser
            coEvery { mockSlugifyBuilder.slugify(articleToCreate.title) } returns slugifiedTitle
            coEvery { mockArticleRepository.create(any()) } returns createdArticle

            // Act
            val result = useCase(validEmail, articleToCreate)

            // Assert
            result.shouldBeInstanceOf<Either.Right<Article>>()
            result.value.slug shouldBe slugifiedTitle
            result.value.title shouldBe articleToCreate.title
        }
    }

    test("should return failure when email is empty") {
        runTest {
            // Arrange
            val emptyEmail = ""
            val articleToCreate = Article(
                title = "Test Article",
                description = "Test description",
                body = "Test body content",
                tagList = listOf("test")
            )

            // Act
            val result = useCase(emptyEmail, articleToCreate)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.InvalidAuthorEmail>()
        }
    }

    test("should handle repository exceptions") {
        runTest {
            // Arrange
            val validEmail = "test@example.com"
            val articleToCreate = Article(
                title = "Test Article",
                description = "Test description",
                body = "Test body content",
                tagList = listOf("test")
            )

            // Mock the user repository to return a user
            val mockUser = mockk<User>()
            coEvery { mockUserRepository.findByEmail(validEmail) } returns mockUser

            // Mock the slugify operation
            val slugifiedTitle = "test-article"
            coEvery { mockSlugifyBuilder.slugify(articleToCreate.title) } returns slugifiedTitle

            // Setup the article repository to throw an exception
            coEvery {
                mockArticleRepository.create(any())
            } throws ArticleException.ArticleCreationFailed

            // Act
            val result = useCase(validEmail, articleToCreate)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.ArticleCreationFailed>()
        }
    }

    // Additional test for user not found scenario
    test("should return UserNotFound failure when user email doesn't exist") {
        runTest {
            // Arrange
            val validEmail = "nonexistent@example.com"
            val articleToCreate = Article(
                title = "Test Article",
                description = "Test description",
                body = "Test body content",
                tagList = listOf("test")
            )

            // Mock the user repository to return null (user not found)
            coEvery { mockUserRepository.findByEmail(validEmail) } returns null

            // Act
            val result = useCase(validEmail, articleToCreate)

            // Assert
            result.shouldBeInstanceOf<Either.Left<ArticleFailure>>()
            result.value.shouldBeInstanceOf<ArticleFailure.UserNotFound>()
            result.value.let { it as ArticleFailure.UserNotFound }.email shouldBe validEmail
        }
    }
})