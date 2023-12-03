package io.aethibo.features.articles.data.repository

import io.aethibo.core.config.DatabaseFactory.dbQuery
import io.aethibo.core.exceptions.NotFoundResponse
import io.aethibo.features.articles.data.table.Articles
import io.aethibo.features.articles.data.table.ArticlesTags
import io.aethibo.features.articles.data.table.Favorites
import io.aethibo.features.articles.domain.model.Article
import io.aethibo.features.articles.domain.repository.ArticlesRepository
import io.aethibo.features.tags.data.table.Tags
import io.aethibo.features.users.data.table.Follows
import io.aethibo.features.users.data.table.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList

class ArticlesRepositoryImpl : ArticlesRepository {
    private suspend fun findWithConditional(where: Op<Boolean>, limit: Int, offset: Long): List<Article> = dbQuery {
        Articles.join(Users, JoinType.INNER, additionalConstraint = { Articles.author eq Users.id })
            .select { where }
            .limit(limit, offset)
            .orderBy(Articles.createdAt, SortOrder.ASC)
            .map { row ->
                val slug = row[Articles.slug]
                val favoritesCount = Favorites.select { Favorites.slug eq slug }.count()
                val tagList = Tags.join(
                    ArticlesTags,
                    JoinType.INNER,
                    additionalConstraint = { Tags.id eq ArticlesTags.tag },
                )
                    .select { ArticlesTags.slug eq slug }
                    .map { it[Tags.name] }
                Articles.toDomain(row, Users.toDomain(row))
                    .copy(
                        favorited = favoritesCount > 0,
                        favoritesCount = favoritesCount,
                        tagList = tagList,
                    )
            }
    }

    override suspend fun findByTag(tag: String, limit: Int, offset: Long): List<Article> {
        val slugs = dbQuery {
            Tags.join(ArticlesTags, JoinType.INNER, additionalConstraint = { Tags.id eq ArticlesTags.tag })
                .select { Tags.name eq tag }
                .map { it[ArticlesTags.slug] }
        }
        return findWithConditional((Articles.slug inList slugs), limit, offset)
    }

    override suspend fun findByFavorited(favorited: String, limit: Int, offset: Long): List<Article> {
        val slugs = dbQuery {
            Favorites.join(Users, JoinType.INNER, additionalConstraint = { Favorites.user eq Users.id })
                .slice(Favorites.slug)
                .select { Users.username eq favorited }
                .map { it[Favorites.slug] }
        }
        return findWithConditional((Articles.slug inList slugs), limit, offset)
    }

    override suspend fun create(article: Article): Article? {
        dbQuery {
            Articles.insert { row ->
                row[slug] = article.slug!!
                row[title] = article.title!!
                row[description] = article.description!!
                row[body] = article.body
                row[createdAt] = System.currentTimeMillis()
                row[updatedAt] = System.currentTimeMillis()
                row[author] = article.author?.id!!
            }
            article.tagList.map { tag ->
                Tags.slice(Tags.id)
                    .select { Tags.name eq tag }
                    .map { row -> row[Tags.id].value }
                    .firstOrNull()
                    ?: Tags.insertAndGetId { it[name] = tag }.value
            }.also {
                ArticlesTags.batchInsert(it) { tagId ->
                    this[ArticlesTags.tag] = tagId
                    this[ArticlesTags.slug] = article.slug!!
                }
            }
        }

        return findBySlug(article.slug!!)
    }

    override suspend fun findAll(limit: Int, offset: Long): List<Article> {
        return dbQuery {
            Articles.join(Users, JoinType.INNER, additionalConstraint = { Articles.author eq Users.id })
                .selectAll()
                .limit(limit, offset)
                .orderBy(Articles.createdAt, SortOrder.ASC)
                .map { row ->
                    val favoritesCount = Favorites.select { Favorites.slug eq row[Articles.slug] }.count()
                    Articles.toDomain(row, Users.toDomain(row))
                        .copy(
                            favoritesCount = favoritesCount,
                            tagList =
                            Tags.join(
                                ArticlesTags,
                                JoinType.INNER,
                                additionalConstraint = { Tags.id eq ArticlesTags.tag },
                            )
                                .select { ArticlesTags.slug eq row[Articles.slug] }
                                .map { it[Tags.name] },
                        )
                }
        }
    }

    override suspend fun findFeed(email: String, limit: Int, offset: Long): List<Article> {
        val authors = dbQuery {
            Follows.join(Users, JoinType.INNER, additionalConstraint = { Follows.follower eq Users.id })
                .slice(Follows.user)
                .select { Users.email eq email }
                .map { it[Follows.user] }
        }

        return findWithConditional((Articles.author inList authors), limit, offset)
    }

    override suspend fun findBySlug(slug: String): Article? {
        return findWithConditional(
            where = (Articles.slug eq slug),
            limit = 1,
            offset = 0
        ).firstOrNull()
    }

    override suspend fun findByAuthor(author: String, limit: Int, offset: Long): List<Article> {
        return findWithConditional((Users.username eq author), limit, offset)
    }

    override suspend fun update(slug: String, article: Article): Article? {
        return dbQuery {
            Articles.update({ Articles.slug eq slug }) { row ->
                if (article.slug != null) {
                    row[Articles.slug] = article.slug
                }
                if (article.title != null) {
                    row[title] = article.title
                }
                if (article.description != null) {
                    row[description] = article.description
                }
                row[body] = article.body
                row[updatedAt] = System.currentTimeMillis()
                if (article.author != null) {
                    row[author] = article.author.id!!
                }
            }
            if (article.slug != null) {
                Favorites.update({ Favorites.slug eq slug }) { row ->
                    row[Favorites.slug] = slug
                }
            }
            Favorites.select {
                Favorites.slug eq article.slug!!
            }.count()
        }.let {
            findBySlug(article.slug!!)?.copy(favoritesCount = it)
        }
    }

    override suspend fun favorite(userId: Long, slug: String): Long {
        return dbQuery {
            Favorites.insert { row ->
                row[Favorites.slug] = slug
                row[user] = userId
            }.let {
                Favorites.select { Favorites.slug eq slug }.count()
            }
        }
    }

    override suspend fun unfavorite(userId: Long, slug: String): Long {
        val article = findBySlug(slug) ?: throw NotFoundResponse()
        return dbQuery {
            Favorites.deleteWhere {
                Favorites.slug eq article.slug!! and (user eq userId)
            }.let {
                Favorites.select { Favorites.slug eq article.slug!! }.count()
            }
        }
    }

    override suspend fun delete(slug: String) {
        dbQuery {
            Articles.deleteWhere { Articles.slug eq slug }
            Favorites.deleteWhere { Favorites.slug eq slug }
        }
    }
}
