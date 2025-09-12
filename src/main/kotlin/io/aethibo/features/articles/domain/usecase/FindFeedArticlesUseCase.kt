package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Email
import io.aethibo.core.utils.Limit
import io.aethibo.core.utils.Offset
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository

fun interface FindFeedUseCase : suspend (Email, Limit, Offset) -> Either<ArticleFailure, List<Article>>

suspend fun findFeed(
    articleRepository: ArticleRepository,
    email: Email,
    limit: Limit,
    offset: Offset
): Either<ArticleFailure, List<Article>> = either {
    catch({
        val validEmail = email?.takeIf { it.isNotBlank() }
            ?: raise(ArticleFailure.InvalidAuthorEmail(email.orEmpty()))

        articleRepository.getFeed(validEmail, limit, offset)
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("feed retrieval", exception)
        }
        raise(failure)
    }
}
