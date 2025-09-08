package io.aethibo.features.articles.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.*
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.failure.ArticleFailure
import io.aethibo.features.articles.data.failure.mapToFailure
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository

fun interface FindArticlesUseCase : suspend (
    Limit,
    Offset,
    Tag,
    Author,
    Favourite
) -> Either<ArticleFailure, List<Article>>

suspend fun findArticles(
    articleRepository: ArticleRepository,
    limit: Limit,
    offset: Offset,
    tag: Tag = null,
    author: Author = null,
    favourite: Favourite = null,
): Either<ArticleFailure, List<Article>> = either {
    catch({
        when {
            !tag.isNullOrBlank() -> articleRepository.getByTag(tag, limit, offset)
            !author.isNullOrBlank() -> articleRepository.getByAuthor(author, limit, offset)
            !favourite.isNullOrBlank() -> articleRepository.getByFavourite(favourite, limit, offset)
            else -> articleRepository.all(limit, offset)
        }
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("article search", exception)
        }
        raise(failure)
    }
}
