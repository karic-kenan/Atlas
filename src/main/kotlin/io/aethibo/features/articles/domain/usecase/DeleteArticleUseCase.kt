package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.repository.ArticleRepository

fun interface DeleteArticleUseCase : suspend (Slug) -> Either<ArticleFailure, Unit>

suspend fun deleteArticle(
    articleRepository: ArticleRepository,
    slug: Slug
): Either<ArticleFailure, Unit> = either {
    catch({
        articleRepository.delete(slug)
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("article deletion", exception)
        }
        raise(failure)
    }
}
