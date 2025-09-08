package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.Slug
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository

fun interface GetArticleBySlugUseCase : suspend (Slug) -> Either<ArticleFailure, Article>

suspend fun getArticleBySlug(
    articleRepository: ArticleRepository,
    slug: String?
): Either<ArticleFailure, Article> = either {
    catch({
        val validSlug = slug?.takeIf { it.isNotBlank() }
            ?: raise(ArticleFailure.InvalidSlug(slug.orEmpty()))

        articleRepository.getBySlug(validSlug)
            ?: raise(ArticleFailure.ArticleNotFound(validSlug))
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("article lookup", exception)
        }
        raise(failure)
    }
}
