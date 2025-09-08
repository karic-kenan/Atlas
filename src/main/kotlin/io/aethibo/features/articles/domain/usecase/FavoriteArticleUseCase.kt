package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Email
import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface FavoriteArticleUseCase : suspend (Email, Slug) -> Either<ArticleFailure, Article>

suspend fun favoriteArticle(
    userRepository: UsersRepository,
    articleRepository: ArticleRepository,
    email: Email,
    slug: Slug
): Either<ArticleFailure, Article> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(ArticleFailure.InvalidAuthorEmail(email.orEmpty()))

        val article = articleRepository.getBySlug(slug)
            ?: raise(ArticleFailure.ArticleNotFound(slug))

        val user = userRepository.findByEmail(validEmail)
            ?: raise(ArticleFailure.UserNotFound(validEmail))

        val userId = user.id
            ?: raise(
                ArticleFailure.DatabaseError(
                    operation = "favorite operation",
                    cause = RuntimeException("User ID is null")
                )
            )

        val favoritesCount = articleRepository.favorite(userId, slug)

        article.copy(favorited = true, favoritesCount = favoritesCount.toLong())
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.FavoriteOperationFailed
        }
        raise(failure)
    }
}
