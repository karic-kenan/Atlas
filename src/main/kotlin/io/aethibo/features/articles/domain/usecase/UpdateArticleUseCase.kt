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

fun interface UpdateArticleUseCase : suspend (Slug, Article) -> Either<ArticleFailure, Article>

suspend fun updateArticle(
    articleRepository: ArticleRepository,
    slug: Slug,
    article: Article,
): Either<ArticleFailure, Article> = either {
    catch({
        val existingArticle = articleRepository.getBySlug(slug)
            ?: raise(ArticleFailure.ArticleNotFound(slug))

        articleRepository.update(
            slug, existingArticle.copy(
                slug = slug,
                title = article.title,
                description = article.description,
                body = article.body,
                tagList = article.tagList
            )
        ) ?: raise(ArticleFailure.ArticleUpdateFailed)
    }) { exception ->
        val failure = when (exception) {
            is ArticleException -> exception.mapToFailure()
            else -> ArticleFailure.DatabaseError("article update", exception)
        }
        raise(failure)
    }
}
