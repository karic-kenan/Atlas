package io.aethibo.features.articles.data.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.articles.data.failure.ArticleException
import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.articles.data.model.ArticleTagsEntity
import io.aethibo.features.articles.data.model.FavoritesEntity
import io.aethibo.features.articles.domain.mapper.toArticleDomain
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.tags.data.model.TagEntity
import io.aethibo.features.users.data.model.FollowsEntity
import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.mapper.toUserDomain
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*

class ArticleRepositoryImpl : ArticleRepository {
    // --- Validation ---
    private fun validateArticleInput(article: Article) {
        if (article.slug.isNullOrBlank()) throw ArticleException.InvalidSlug(article.slug ?: "")
        if (article.title.isNullOrBlank()) throw ArticleException.EmptyTitle
        if (article.description.isNullOrBlank()) throw ArticleException.EmptyDescription
        if (article.body.isBlank()) throw ArticleException.EmptyBody
        if (article.author?.id == null) throw ArticleException.AuthorNotFound(article.author?.id ?: -1)
    }

    private fun validatePagination(limit: Int, offset: Long) {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)
    }

    // --- Create ---
    override suspend fun create(article: Article): Article? {
        return try {
            validateArticleInput(article)

            getBySlug(article.slug!!)?.let {
                throw ArticleException.ArticleAlreadyExists(article.slug)
            }

            dbQuery {
                ArticleEntity.insert { row ->
                    row[slug] = article.slug
                    row[title] = article.title!!
                    row[description] = article.description!!
                    row[body] = article.body
                    row[author] = article.author?.id!!
                }

                article.tagList.map { tag ->
                    TagEntity.select(TagEntity.id)
                        .where { TagEntity.name eq tag }
                        .map { row -> row[TagEntity.id].value }
                        .firstOrNull()
                        ?: TagEntity.insertAndGetId { it[name] = tag }.value
                }.also {
                    ArticleTagsEntity.batchInsert(it) { tagId ->
                        this[ArticleTagsEntity.tag] = tagId
                        this[ArticleTagsEntity.slug] = article.slug
                    }
                }
            }

            getBySlug(article.slug) ?: throw ArticleException.ArticleCreationFailed
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("create", e)
        }
    }

    // --- Read ---
    override suspend fun all(limit: Int, offset: Long): List<Article> {
        return try {
            validatePagination(limit, offset)

            val articles = dbQuery {
                ArticleEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { ArticleEntity.author eq UserEntity.id })
                    .selectAll()
                    .limit(limit)
                    .offset(offset)
                    .orderBy(ArticleEntity.createdAt, SortOrder.ASC)
                    .map { row ->
                        val favoritesCount =
                            FavoritesEntity.selectAll().where { FavoritesEntity.slug eq row[ArticleEntity.slug] }
                                .count()
                        row.toArticleDomain(row.toUserDomain()).copy(
                            favoritesCount = favoritesCount,
                            tagList =
                                TagEntity.join(
                                    ArticleTagsEntity,
                                    JoinType.INNER,
                                    additionalConstraint = { TagEntity.id eq ArticleTagsEntity.tag },
                                )
                                    .selectAll()
                                    .where { ArticleTagsEntity.slug eq row[ArticleEntity.slug] }
                                    .map { it[TagEntity.name] },
                        )
                    }
            }

            if (articles.isEmpty()) throw ArticleException.EmptySearchResults
            articles
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findAll", e)
        }
    }

    override suspend fun getBySlug(slug: String): Article? {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

            findWithConditional(
                where = (ArticleEntity.slug eq slug),
                limit = 1,
                offset = 0
            ).firstOrNull()
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findBySlug", e)
        }
    }

    override suspend fun getByAuthor(author: String, limit: Int, offset: Long): List<Article> {
        return try {
            validatePagination(limit, offset)
            if (author.isBlank()) throw ArticleException.InvalidUsername(author)

            val articles = findWithConditional((UserEntity.username eq author), limit, offset)
            if (articles.isEmpty()) throw ArticleException.EmptySearchResults
            articles
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findByAuthor", e)
        }
    }

    override suspend fun getByTag(tag: String, limit: Int, offset: Long): List<Article> {
        return try {
            validatePagination(limit, offset)
            if (tag.isBlank()) throw ArticleException.TagNotFound(tag)

            val slugs = dbQuery {
                TagEntity.join(
                    ArticleTagsEntity,
                    JoinType.INNER,
                    additionalConstraint = { TagEntity.id eq ArticleTagsEntity.tag })
                    .selectAll()
                    .where { TagEntity.name eq tag }
                    .map { it[ArticleTagsEntity.slug] }
            }

            if (slugs.isEmpty()) throw ArticleException.EmptySearchResults

            findWithConditional((ArticleEntity.slug inList slugs), limit, offset)
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findByTag", e)
        }
    }

    override suspend fun getByFavourite(favourite: String, limit: Int, offset: Long): List<Article> {
        return try {
            validatePagination(limit, offset)
            if (favourite.isBlank()) throw ArticleException.InvalidUsername(favourite)

            val slugs = dbQuery {
                FavoritesEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { FavoritesEntity.user eq UserEntity.id })
                    .select(FavoritesEntity.slug)
                    .where { UserEntity.username eq favourite }
                    .map { it[FavoritesEntity.slug] }
            }

            if (slugs.isEmpty()) throw ArticleException.EmptySearchResults

            findWithConditional((ArticleEntity.slug inList slugs), limit, offset)
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findByFavorited", e)
        }
    }

    override suspend fun getFeed(email: String, limit: Int, offset: Long): List<Article> {
        return try {
            validatePagination(limit, offset)
            if (email.isBlank() || !email.contains("@")) throw ArticleException.InvalidAuthorEmail(email)

            val authors = dbQuery {
                FollowsEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { FollowsEntity.follower eq UserEntity.id })
                    .select(FollowsEntity.user)
                    .where { UserEntity.email eq email }
                    .map { it[FollowsEntity.user] }
            }

            if (authors.isEmpty()) throw ArticleException.NoFollowedAuthors

            findWithConditional((ArticleEntity.author inList authors), limit, offset)
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("findFeed", e)
        }
    }

    // --- Update ---
    override suspend fun update(slug: String, article: Article): Article? {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

            // Verify article exists
            getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            dbQuery {
                ArticleEntity.update({ ArticleEntity.slug eq slug }) { row ->
                    if (article.slug != null) {
                        row[ArticleEntity.slug] = article.slug
                    }
                    if (article.title != null) {
                        row[title] = article.title
                    }
                    if (article.description != null) {
                        row[description] = article.description
                    }
                    row[body] = article.body
                    if (article.author != null) {
                        row[author] = article.author.id!!
                    }
                }
                if (article.slug != null) {
                    FavoritesEntity.update({ FavoritesEntity.slug eq slug }) { row ->
                        row[FavoritesEntity.slug] = article.slug
                    }
                }
                FavoritesEntity.selectAll()
                    .where { FavoritesEntity.slug eq (article.slug ?: slug) }
                    .count()
            }.let { favCount ->
                getBySlug(article.slug ?: slug)?.copy(favoritesCount = favCount)
                    ?: throw ArticleException.ArticleUpdateFailed
            }
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("update", e)
        }
    }

    // --- Delete ---
    override suspend fun delete(slug: String) {
        try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

            // Verify article exists
            getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            dbQuery {
                // Delete favorites first (foreign key constraint)
                FavoritesEntity.deleteWhere { FavoritesEntity.slug eq slug }
                // Delete article-tag relationships
                ArticleTagsEntity.deleteWhere { ArticleTagsEntity.slug eq slug }
                // Finally delete the article
                ArticleEntity.deleteWhere { ArticleEntity.slug eq slug }
            }
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("delete", e)
        }
    }

    // --- Special actions ---
    override suspend fun favorite(userId: Long, slug: String): Long {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)
            if (userId <= 0) throw ArticleException.AuthorNotFound(userId)

            // Verify article exists
            getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            // Check if already favorited
            val alreadyFavorited = dbQuery {
                FavoritesEntity.selectAll()
                    .where { (FavoritesEntity.slug eq slug) and (FavoritesEntity.user eq userId) }
                    .count() > 0
            }

            if (alreadyFavorited) throw ArticleException.ArticleAlreadyFavorited(slug, userId)

            dbQuery {
                FavoritesEntity.insert { row ->
                    row[FavoritesEntity.slug] = slug
                    row[user] = userId
                }
                FavoritesEntity.selectAll().where { FavoritesEntity.slug eq slug }.count()
            }
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("favorite", e)
        }
    }

    override suspend fun unfavorite(userId: Long, slug: String): Long {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)
            if (userId <= 0) throw ArticleException.AuthorNotFound(userId)

            // Verify article exists
            val article = getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            // Check if actually favorited
            val isFavorited = dbQuery {
                FavoritesEntity.selectAll()
                    .where { (FavoritesEntity.slug eq slug) and (FavoritesEntity.user eq userId) }
                    .count() > 0
            }

            if (!isFavorited) throw ArticleException.ArticleNotFavorited(slug, userId)

            dbQuery {
                FavoritesEntity.deleteWhere {
                    FavoritesEntity.slug eq article.slug!! and (user eq userId)
                }
                FavoritesEntity.selectAll().where { FavoritesEntity.slug eq article.slug!! }.count()
            }
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("unfavorite", e)
        }
    }

    // --- Helper ---
    private suspend fun findWithConditional(where: Op<Boolean>, limit: Int, offset: Long): List<Article> = dbQuery {
        ArticleEntity.join(UserEntity, JoinType.INNER, additionalConstraint = { ArticleEntity.author eq UserEntity.id })
            .selectAll()
            .where { where }
            .limit(limit)
            .offset(offset)
            .orderBy(ArticleEntity.createdAt, SortOrder.ASC)
            .map { row ->
                val slug = row[ArticleEntity.slug]
                val favoritesCount = FavoritesEntity.selectAll().where { FavoritesEntity.slug eq slug }.count()
                val tagList = TagEntity.join(
                    ArticleTagsEntity,
                    JoinType.INNER,
                    additionalConstraint = { TagEntity.id eq ArticleTagsEntity.tag },
                )
                    .selectAll()
                    .where { ArticleTagsEntity.slug eq slug }
                    .map { it[TagEntity.name] }

                row.toArticleDomain(row.toUserDomain())
                    .copy(
                        favorited = favoritesCount > 0,
                        favoritesCount = favoritesCount,
                        tagList = tagList,
                    )
            }
    }
}
