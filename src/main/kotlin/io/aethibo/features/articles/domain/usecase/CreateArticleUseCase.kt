package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.github.slugify.Slugify
import io.aethibo.core.utils.Email
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.users.domain.repository.UsersRepository

fun interface CreateArticleUseCase : suspend (Email, Article) -> Either<ArticleFailure, Article>

suspend fun createArticle(
    userRepository: UsersRepository,
    articleRepository: ArticleRepository,
    slugifyBuilder: Slugify,
    email: Email,
    article: Article
): Either<ArticleFailure, Article> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(ArticleFailure.InvalidAuthorEmail(email.orEmpty()))

        val author = userRepository.findByEmail(validEmail)
            ?: raise(ArticleFailure.UserNotFound(validEmail))

        val articleToCreate = article.copy(
            slug = slugifyBuilder.slugify(article.title),
            author = author
        )

        articleRepository.create(articleToCreate)
            ?: raise(ArticleFailure.ArticleCreationFailed)
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("article creation", exception)
        }
        raise(failure)
    }
}
