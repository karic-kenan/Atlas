package io.aethibo.features.articles.presentation

import com.github.slugify.Slugify
import io.aethibo.core.exceptions.BadRequestResponse
import io.aethibo.core.exceptions.InternalServerErrorResponse
import io.aethibo.core.exceptions.NotFoundResponse
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticlesRepository
import io.aethibo.features.articles.domain.service.ArticlesService
import io.aethibo.features.users.domain.repository.UsersRepository

class ArticlesServiceImpl(
    private val articleRepository: ArticlesRepository,
    private val userRepository: UsersRepository,
    private val slugifyBuilder: Slugify,
) : ArticlesService {
    override suspend fun findBy(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Long
    ): List<Article> {
        return when {
            !tag.isNullOrBlank() -> articleRepository.findByTag(tag, limit, offset)
            !author.isNullOrBlank() -> articleRepository.findByAuthor(author, limit, offset)
            !favorited.isNullOrBlank() -> articleRepository.findByFavorited(favorited, limit, offset)
            else -> articleRepository.findAll(limit, offset)
        }
    }

    override suspend fun create(email: String?, article: Article): Article {
        email ?: throw BadRequestResponse("Invalid user to create article")
        return userRepository.findByEmail(email).let { author ->
            author ?: throw BadRequestResponse("Invalid user to create article")
            articleRepository.create(
                article.copy(slug = slugifyBuilder.slugify(article.title), author = author),
            )
                ?: throw InternalServerErrorResponse("Error to create article.")
        }
    }

    override suspend fun findBySlug(slug: String): Article {
        return articleRepository.findBySlug(slug) ?: throw NotFoundResponse()
    }

    override suspend fun update(slug: String, article: Article): Article? {
        return findBySlug(slug).run {
            articleRepository.update(slug, article.copy(slug = slug))
        }
    }

    override suspend fun findFeed(email: String?, limit: Int, offset: Long): List<Article> {
        email ?: throw BadRequestResponse("invalid user to find feeds")
        return articleRepository.findFeed(email, limit, offset)
    }

    override suspend fun favorite(email: String?, slug: String): Article {
        email ?: throw BadRequestResponse("invalid user to favorite article")
        val article = findBySlug(slug) ?: throw NotFoundResponse()
        return userRepository.findByEmail(email).let { user ->
            user ?: throw BadRequestResponse()
            articleRepository.favorite(user.id!!, slug)
                .let { favoritesCount ->
                    article.copy(favorited = true, favoritesCount = favoritesCount.toLong())
                }
        }
    }

    override suspend fun unfavorite(email: String?, slug: String): Article {
        email ?: throw BadRequestResponse("invalid user to unfavorite article")
        val article = findBySlug(slug) ?: throw NotFoundResponse()
        return userRepository.findByEmail(email).let { user ->
            user ?: throw BadRequestResponse()
            articleRepository.unfavorite(user.id!!, slug)
                .let { favoritesCount ->
                    article.copy(favorited = false, favoritesCount = favoritesCount.toLong())
                }
        }
    }

    override suspend fun delete(slug: String) {
        return articleRepository.delete(slug)
    }
}
