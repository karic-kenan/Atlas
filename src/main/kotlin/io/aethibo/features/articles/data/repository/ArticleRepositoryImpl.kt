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
    // --- Validation helpers ---
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
    override suspend fun create(article: Article): Article? = try {
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

            // Handle tags
            val tagIds = article.tagList.map { tag ->
                TagEntity.select(TagEntity.id)
                    .where { TagEntity.name eq tag }
                    .map { it[TagEntity.id].value }
                    .firstOrNull()
                    ?: TagEntity.insertAndGetId { it[name] = tag }.value
            }

            ArticleTagsEntity.batchInsert(tagIds) { tagId ->
                this[ArticleTagsEntity.tag] = tagId
                this[ArticleTagsEntity.slug] = article.slug
            }
        }

        getBySlug(article.slug) ?: throw ArticleException.ArticleCreationFailed
    } catch (e: ArticleException) {
        throw e
    } catch (e: Exception) {
        throw ArticleException.DatabaseError("create", e)
    }

    // --- Read ---
    override suspend fun all(limit: Int, offset: Long): List<Article> {
        validatePagination(limit, offset)
        return findWithConditional(Op.TRUE, limit, offset)
    }

    override suspend fun getBySlug(slug: String): Article? {
        if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)
        return findWithConditional(ArticleEntity.slug eq slug, limit = 1, offset = 0).firstOrNull()
    }

    override suspend fun getByAuthor(author: String, limit: Int, offset: Long): List<Article> {
        validatePagination(limit, offset)
        if (author.isBlank()) throw ArticleException.InvalidUsername(author)

        return findWithConditional(UserEntity.username eq author, limit, offset)
    }

    override suspend fun getByTag(tag: String, limit: Int, offset: Long): List<Article> {
        validatePagination(limit, offset)
        if (tag.isBlank()) throw ArticleException.TagNotFound(tag)

        val slugs = dbQuery {
            ArticleTagsEntity.join(
                TagEntity,
                JoinType.INNER,
                additionalConstraint = { ArticleTagsEntity.tag eq TagEntity.id })
                .selectAll()
                .where { TagEntity.name eq tag }
                .map { it[ArticleTagsEntity.slug] }
        }

        if (slugs.isEmpty()) return emptyList()

        return findWithConditional(ArticleEntity.slug inList slugs, limit, offset)
    }

    override suspend fun getByFavourite(favourite: String, limit: Int, offset: Long): List<Article> {
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

        if (slugs.isEmpty()) return emptyList()

        return findWithConditional(ArticleEntity.slug inList slugs, limit, offset)
    }

    override suspend fun getFeed(email: String, limit: Int, offset: Long): List<Article> {
        validatePagination(limit, offset)
        if (email.isBlank() || !email.contains("@")) throw ArticleException.InvalidAuthorEmail(email)

        val authorIds = dbQuery {
            FollowsEntity.join(
                UserEntity,
                JoinType.INNER,
                additionalConstraint = { FollowsEntity.follower eq UserEntity.id })
                .select(FollowsEntity.user)
                .where { UserEntity.email eq email }
                .map { it[FollowsEntity.user] }
        }

        if (authorIds.isEmpty()) return emptyList()

        return findWithConditional(ArticleEntity.author inList authorIds, limit, offset)
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
    private suspend fun findWithConditional(
        where: Op<Boolean>,
        limit: Int,
        offset: Long
    ): List<Article> = dbQuery {
        // 1️⃣ Load articles with authors
        val articlesWithAuthors = ArticleEntity.join(
            UserEntity,
            JoinType.INNER,
            additionalConstraint = { ArticleEntity.author eq UserEntity.id })
            .selectAll()
            .where { where }
            .limit(limit)
            .offset(offset)
            .orderBy(ArticleEntity.createdAt, SortOrder.ASC)
            .map { it.toArticleDomain(it.toUserDomain()) }

        val slugs = articlesWithAuthors.map { it.slug!! }
        if (slugs.isEmpty()) return@dbQuery emptyList()

        // 2️⃣ Preload favorites
        val favoritesMap = FavoritesEntity.selectAll().where { FavoritesEntity.slug inList slugs }
            .groupBy { it[FavoritesEntity.slug] }
            .mapValues { it.value.size }

        // 3️⃣ Preload tags
        val tagsMap = ArticleTagsEntity.join(
            TagEntity,
            JoinType.INNER,
            additionalConstraint = { ArticleTagsEntity.tag eq TagEntity.id })
            .selectAll()
            .where { ArticleTagsEntity.slug inList slugs }
            .groupBy { it[ArticleTagsEntity.slug] }
            .mapValues { entry -> entry.value.map { it[TagEntity.name] } }

        // 4️⃣ Map counts and tags
        articlesWithAuthors.map { article ->
            article.copy(
                favorited = (favoritesMap[article.slug] ?: 0) > 0,
                favoritesCount = (favoritesMap[article.slug] ?: 0).toLong(),
                tagList = tagsMap[article.slug] ?: emptyList()
            )
        }
    }
}
