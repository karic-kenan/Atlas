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
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greater
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

data class CursorPagination(
    val cursor: String? = null,
    val limit: Int = 20,
    val direction: Direction = Direction.FORWARD
) {
    enum class Direction { FORWARD, BACKWARD }

    fun validateLimit() {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
    }
}

data class PaginatedResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val previousCursor: String?,
    val hasMore: Boolean,
    val totalCount: Long? = null
)

class ArticleRepositoryImpl(
    meterRegistry: MeterRegistry
) : ArticleRepository {

    // --- Performance Monitoring ---
    private val dbTimer = Timer.builder("article.database.query.time")
        .description("Time spent executing database queries")
        .register(meterRegistry)

    private val cacheHitCounter = Counter.builder("article.cache.hits")
        .description("Number of cache hits")
        .register(meterRegistry)

    private val cacheMissCounter = Counter.builder("article.cache.misses")
        .description("Number of cache misses")
        .register(meterRegistry)

    private val queryCounter = Counter.builder("article.database.queries")
        .description("Total number of database queries executed")
        .register(meterRegistry)

    // --- Caching Infrastructure ---
    private data class CacheKey(
        val type: String,
        val params: String
    )

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Instant,
        val ttl: Long = 300_000 // 5 minutes default TTL
    ) {
        fun isExpired(): Boolean =
            Instant.now().toEpochMilli() - timestamp.toEpochMilli() > ttl
    }

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry<*>>()
    private val cacheMutex = Mutex()

    private suspend fun <T> withCache(
        cacheKey: CacheKey,
        ttl: Long = 300_000,
        supplier: suspend () -> T
    ): T {
        cacheMutex.withLock {
            val cached = cache[cacheKey] as? CacheEntry<T>
            if (cached != null && !cached.isExpired()) {
                cacheHitCounter.increment()
                return cached.data
            }
        }

        cacheMissCounter.increment()
        val result = supplier()

        cacheMutex.withLock {
            cache[cacheKey] = CacheEntry(result, Instant.now(), ttl)
        }

        return result
    }

    private suspend fun invalidateCache(pattern: String) {
        cacheMutex.withLock {
            cache.keys.removeIf { it.type.contains(pattern) || it.params.contains(pattern) }
        }
    }

    // --- Enhanced Database Query Wrapper with Manual Timer ---
    private suspend fun <T> timedDbQuery(
        queryName: String,
        block: suspend () -> T
    ): T {
        queryCounter.increment()

        val startTime = System.currentTimeMillis()
        return try {
            val result = dbQuery { block() }
            val duration = System.currentTimeMillis() - startTime

            // Record timing manually
            dbTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            // Log slow queries (> 500ms)
            if (duration > 500) {
                println("SLOW QUERY DETECTED: $queryName took ${duration}ms")
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            dbTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)
            println("QUERY FAILED: $queryName failed after ${duration}ms - ${e.message}")
            throw e
        }
    }

    // --- Validation helpers ---
    private fun validateArticleInput(article: Article) {
        if (article.slug.isNullOrBlank()) throw ArticleException.InvalidSlug(article.slug ?: "")
        if (article.title.isNullOrBlank()) throw ArticleException.EmptyTitle
        if (article.description.isNullOrBlank()) throw ArticleException.EmptyDescription
        if (article.body.isBlank()) throw ArticleException.EmptyBody
        if (article.author?.id == null) throw ArticleException.AuthorNotFound(article.author?.id ?: -1)
    }

    // --- Cursor-based Pagination Helpers ---
    private fun encodeCursor(createdAt: Instant, slug: String): String {
        return "${createdAt.toEpochMilli()}:$slug".let {
            java.util.Base64.getEncoder().encodeToString(it.toByteArray())
        }
    }

    private fun decodeCursor(cursor: String): Pair<Instant, String>? {
        return try {
            val decoded = String(java.util.Base64.getDecoder().decode(cursor))
            val parts = decoded.split(":", limit = 2)
            if (parts.size == 2) {
                Instant.ofEpochMilli(parts[0].toLong()) to parts[1]
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // --- Optimized Create (No N+1) ---
    override suspend fun create(article: Article): Article? = try {
        validateArticleInput(article)

        getBySlug(article.slug!!)?.let {
            throw ArticleException.ArticleAlreadyExists(article.slug)
        }

        timedDbQuery("create_article") {
            // Insert article and get tags in single transaction
            ArticleEntity.insert { row ->
                row[slug] = article.slug
                row[title] = article.title!!
                row[description] = article.description!!
                row[body] = article.body
                row[author] = article.author?.id!!
            }

            // Optimized tag handling - batch operations
            if (article.tagList.isNotEmpty()) {
                val existingTags = TagEntity.selectAll()
                    .where { TagEntity.name inList article.tagList }
                    .associate { it[TagEntity.name] to it[TagEntity.id].value }

                val newTags = article.tagList - existingTags.keys
                val newTagIds = if (newTags.isNotEmpty()) {
                    TagEntity.batchInsert(newTags) { tag ->
                        this[TagEntity.name] = tag
                    }.map { it[TagEntity.id].value }
                } else emptyList()

                val allTagIds = existingTags.values + newTagIds

                ArticleTagsEntity.batchInsert(allTagIds) { tagId ->
                    this[ArticleTagsEntity.tag] = tagId
                    this[ArticleTagsEntity.slug] = article.slug
                }
            }
        }

        // Invalidate relevant caches
        invalidateCache("article")
        invalidateCache("tag:${article.tagList.joinToString(",")}")

        getBySlug(article.slug) ?: throw ArticleException.ArticleCreationFailed
    } catch (e: ArticleException) {
        throw e
    } catch (e: Exception) {
        throw ArticleException.DatabaseError("create", e)
    }

    // --- Cursor-based Pagination Implementation ---
    suspend fun allWithCursor(pagination: CursorPagination): PaginatedResult<Article> {
        pagination.validateLimit()

        val cacheKey = CacheKey("articles_cursor", "${pagination.cursor}:${pagination.limit}:${pagination.direction}")

        return withCache(cacheKey, ttl = 60_000) { // 1 minute cache
            timedDbQuery("all_articles_cursor") {
                val (cursorTime, cursorSlug) = pagination.cursor?.let { decodeCursor(it) }
                    ?: (Instant.MIN to "")

                val cursorDateTime = LocalDateTime.ofInstant(cursorTime, java.time.ZoneOffset.UTC)

                val whereCondition = when (pagination.direction) {
                    CursorPagination.Direction.FORWARD -> {
                        if (pagination.cursor == null) Op.TRUE
                        else (ArticleEntity.createdAt greater cursorDateTime) or
                                ((ArticleEntity.createdAt eq cursorDateTime) and (ArticleEntity.slug greater cursorSlug))
                    }

                    CursorPagination.Direction.BACKWARD -> {
                        (ArticleEntity.createdAt less cursorDateTime) or
                                ((ArticleEntity.createdAt eq cursorDateTime) and (ArticleEntity.slug less cursorSlug))
                    }
                }

                val articles = findWithConditionalOptimized(
                    whereCondition,
                    pagination.limit + 1, // +1 to check if there are more results
                    0,
                    pagination.direction == CursorPagination.Direction.BACKWARD
                )

                val hasMore = articles.size > pagination.limit
                val resultItems = articles.take(pagination.limit)

                val nextCursor = if (hasMore && pagination.direction == CursorPagination.Direction.FORWARD) {
                    resultItems.lastOrNull()?.let {
                        val instant = it.createdAt?.atZone(ZoneOffset.UTC)?.toInstant() ?: Instant.now()
                        encodeCursor(instant, it.slug!!)
                    }
                } else null

                val previousCursor =
                    if (resultItems.isNotEmpty() && pagination.direction == CursorPagination.Direction.FORWARD) {
                        resultItems.firstOrNull()?.let {
                            val instant = it.createdAt?.atZone(ZoneOffset.UTC)?.toInstant() ?: Instant.now()
                            encodeCursor(instant, it.slug!!)
                        }
                    } else null

                PaginatedResult(
                    items = resultItems,
                    nextCursor = nextCursor,
                    previousCursor = previousCursor,
                    hasMore = hasMore
                )
            }
        }
    }

    // --- Legacy offset-based pagination (kept for backward compatibility) ---
    override suspend fun all(limit: Int, offset: Long): List<Article> {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)

        val cacheKey = CacheKey("articles_offset", "$limit:$offset")
        return withCache(cacheKey) {
            findWithConditionalOptimized(Op.TRUE, limit, offset)
        }
    }

    override suspend fun getBySlug(slug: String): Article? {
        if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

        val cacheKey = CacheKey("article_by_slug", slug)
        return withCache(cacheKey) {
            timedDbQuery("get_by_slug") {
                findWithConditionalOptimized(ArticleEntity.slug eq slug, limit = 1, offset = 0).firstOrNull()
            }
        }
    }

    override suspend fun getByAuthor(author: String, limit: Int, offset: Long): List<Article> {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)
        if (author.isBlank()) throw ArticleException.InvalidUsername(author)

        val cacheKey = CacheKey("articles_by_author", "$author:$limit:$offset")
        return withCache(cacheKey) {
            findWithConditionalOptimized(UserEntity.username eq author, limit, offset)
        }
    }

    override suspend fun getByTag(tag: String, limit: Int, offset: Long): List<Article> {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)
        if (tag.isBlank()) throw ArticleException.TagNotFound(tag)

        val cacheKey = CacheKey("articles_by_tag", "$tag:$limit:$offset")
        return withCache(cacheKey) {
            timedDbQuery("get_by_tag") {
                // Single optimized query instead of multiple queries
                ArticleEntity
                    .join(UserEntity, JoinType.INNER, ArticleEntity.author, UserEntity.id)
                    .join(ArticleTagsEntity, JoinType.INNER, ArticleEntity.slug, ArticleTagsEntity.slug)
                    .join(TagEntity, JoinType.INNER, ArticleTagsEntity.tag, TagEntity.id)
                    .selectAll()
                    .where { TagEntity.name eq tag }
                    .limit(limit)
                    .offset(offset)
                    .orderBy(ArticleEntity.createdAt, SortOrder.DESC)
                    .let { query ->
                        val articlesWithAuthors = query.map { it.toArticleDomain(it.toUserDomain()) }
                        enrichWithMetadata(articlesWithAuthors)
                    }
            }
        }
    }

    override suspend fun getByFavourite(favourite: String, limit: Int, offset: Long): List<Article> {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)
        if (favourite.isBlank()) throw ArticleException.InvalidUsername(favourite)

        val cacheKey = CacheKey("articles_by_favourite", "$favourite:$limit:$offset")
        return withCache(cacheKey) {
            timedDbQuery("get_by_favourite") {
                // Single optimized query
                val favUser = UserEntity.alias("favUser")
                ArticleEntity
                    .join(UserEntity, JoinType.INNER, ArticleEntity.author, UserEntity.id)
                    .join(FavoritesEntity, JoinType.INNER, ArticleEntity.slug, FavoritesEntity.slug)
                    .join(favUser, JoinType.INNER, FavoritesEntity.user, favUser[UserEntity.id])
                    .selectAll()
                    .where { favUser[UserEntity.username] eq favourite }
                    .limit(limit)
                    .offset(offset)
                    .orderBy(ArticleEntity.createdAt, SortOrder.DESC)
                    .let { query ->
                        val articlesWithAuthors = query.map { it.toArticleDomain(it.toUserDomain()) }
                        enrichWithMetadata(articlesWithAuthors)
                    }
            }
        }
    }

    override suspend fun getFeed(email: String, limit: Int, offset: Long): List<Article> {
        if (limit !in 1..100) throw ArticleException.InvalidLimit(limit)
        if (offset < 0) throw ArticleException.InvalidOffset(offset)
        if (email.isBlank() || !email.contains("@")) throw ArticleException.InvalidAuthorEmail(email)

        val cacheKey = CacheKey("feed", "$email:$limit:$offset")
        return withCache(cacheKey, ttl = 60_000) { // 1 minute cache for feeds
            timedDbQuery("get_feed") {
                // Single optimized query for feed
                val author = UserEntity.alias("author")
                val follower = UserEntity.alias("follower")

                ArticleEntity
                    .join(author, JoinType.INNER, ArticleEntity.author, author[UserEntity.id])
                    .join(FollowsEntity, JoinType.INNER, ArticleEntity.author, FollowsEntity.user)
                    .join(follower, JoinType.INNER, FollowsEntity.follower, follower[UserEntity.id])
                    .selectAll()
                    .where { follower[UserEntity.email] eq email }
                    .limit(limit)
                    .offset(offset)
                    .orderBy(ArticleEntity.createdAt, SortOrder.DESC)
                    .let { query ->
                        val articlesWithAuthors = query.map {
                            // Pass the alias to the mapper if it supports it
                            it.toArticleDomain(it.toUserDomain())
                        }
                        enrichWithMetadata(articlesWithAuthors)
                    }
            }
        }
    }

    // --- Update with cache invalidation ---
    override suspend fun update(slug: String, article: Article): Article? {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

            // Verify article exists
            getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            val result = timedDbQuery("update_article") {
                ArticleEntity.update({ ArticleEntity.slug eq slug }) { row ->
                    if (article.slug != null) row[ArticleEntity.slug] = article.slug
                    if (article.title != null) row[ArticleEntity.title] = article.title
                    if (article.description != null) row[ArticleEntity.description] = article.description
                    row[ArticleEntity.body] = article.body
                    if (article.author != null) row[ArticleEntity.author] = article.author.id!!
                }

                if (article.slug != null) {
                    FavoritesEntity.update({ FavoritesEntity.slug eq slug }) { row ->
                        row[FavoritesEntity.slug] = article.slug
                    }
                }

                val favCount = FavoritesEntity.selectAll()
                    .where { FavoritesEntity.slug eq (article.slug ?: slug) }
                    .count()

                getBySlug(article.slug ?: slug)?.copy(favoritesCount = favCount)
                    ?: throw ArticleException.ArticleUpdateFailed
            }

            // Invalidate caches
            invalidateCache(slug)
            if (article.slug != null) invalidateCache(article.slug)
            invalidateCache("articles")

            result
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("update", e)
        }
    }

    // --- Delete with cache invalidation ---
    override suspend fun delete(slug: String) {
        try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)

            // Verify article exists and get tags for cache invalidation
            val article = getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            timedDbQuery("delete_article") {
                // Delete in proper order due to foreign key constraints
                FavoritesEntity.deleteWhere { FavoritesEntity.slug eq slug }
                ArticleTagsEntity.deleteWhere { ArticleTagsEntity.slug eq slug }
                ArticleEntity.deleteWhere { ArticleEntity.slug eq slug }
            }

            // Invalidate caches
            invalidateCache(slug)
            invalidateCache("articles")
            article.tagList.forEach { tag -> invalidateCache("tag:$tag") }
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("delete", e)
        }
    }

    // --- Favorite/Unfavorite with cache invalidation ---
    override suspend fun favorite(userId: Long, slug: String): Long {
        return try {
            if (slug.isBlank()) throw ArticleException.InvalidSlug(slug)
            if (userId <= 0) throw ArticleException.AuthorNotFound(userId)

            getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            val result = timedDbQuery("favorite_article") {
                val alreadyFavorited = FavoritesEntity.selectAll()
                    .where { (FavoritesEntity.slug eq slug) and (FavoritesEntity.user eq userId) }
                    .count() > 0

                if (alreadyFavorited) throw ArticleException.ArticleAlreadyFavorited(slug, userId)

                FavoritesEntity.insert { row ->
                    row[FavoritesEntity.slug] = slug
                    row[FavoritesEntity.user] = userId
                }

                FavoritesEntity.selectAll().where { FavoritesEntity.slug eq slug }.count()
            }

            // Invalidate caches
            invalidateCache(slug)
            invalidateCache("favourite")

            result
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

            val article = getBySlug(slug) ?: throw ArticleException.ArticleNotFound(slug)

            val result = timedDbQuery("unfavorite_article") {
                val isFavorited = FavoritesEntity.selectAll()
                    .where { (FavoritesEntity.slug eq slug) and (FavoritesEntity.user eq userId) }
                    .count() > 0

                if (!isFavorited) throw ArticleException.ArticleNotFavorited(slug, userId)

                FavoritesEntity.deleteWhere {
                    (FavoritesEntity.slug eq article.slug!!) and (FavoritesEntity.user eq userId)
                }

                FavoritesEntity.selectAll().where { FavoritesEntity.slug eq article.slug!! }.count()
            }

            // Invalidate caches
            invalidateCache(slug)
            invalidateCache("favourite")

            result
        } catch (e: ArticleException) {
            throw e
        } catch (e: Exception) {
            throw ArticleException.DatabaseError("unfavorite", e)
        }
    }

    // --- Optimized Helper Function (Eliminates N+1) ---
    private suspend fun findWithConditionalOptimized(
        where: Op<Boolean>,
        limit: Int,
        offset: Long,
        reversed: Boolean = false
    ): List<Article> = timedDbQuery("find_with_conditional") {
        // Load articles with authors in single query
        val query = ArticleEntity.join(
            UserEntity,
            JoinType.INNER,
            additionalConstraint = { ArticleEntity.author eq UserEntity.id })
            .selectAll()
            .where { where }
            .limit(limit)
            .offset(offset)

        val finalQuery = if (reversed) {
            query.orderBy(ArticleEntity.createdAt, SortOrder.DESC)
        } else {
            query.orderBy(ArticleEntity.createdAt, SortOrder.ASC)
        }

        val articlesWithAuthors = finalQuery.map { it.toArticleDomain(it.toUserDomain()) }

        if (articlesWithAuthors.isEmpty()) return@timedDbQuery emptyList()

        enrichWithMetadata(articlesWithAuthors)
    }

    private suspend fun enrichWithMetadata(articles: List<Article>): List<Article> {
        val slugs = articles.map { it.slug!! }

        // Preload favorites count in single query
        val favoritesMap = FavoritesEntity.selectAll()
            .where { FavoritesEntity.slug inList slugs }
            .groupBy { it[FavoritesEntity.slug] }
            .mapValues { it.value.size }

        // Preload tags in single query
        val tagsMap = ArticleTagsEntity.join(
            TagEntity,
            JoinType.INNER,
            additionalConstraint = { ArticleTagsEntity.tag eq TagEntity.id })
            .selectAll()
            .where { ArticleTagsEntity.slug inList slugs }
            .groupBy { it[ArticleTagsEntity.slug] }
            .mapValues { entry -> entry.value.map { it[TagEntity.name] } }

        // Combine all data
        return articles.map { article ->
            article.copy(
                favorited = (favoritesMap[article.slug] ?: 0) > 0,
                favoritesCount = (favoritesMap[article.slug] ?: 0).toLong(),
                tagList = tagsMap[article.slug] ?: emptyList()
            )
        }
    }

    // --- Cache Management ---
    suspend fun clearCache() {
        cacheMutex.withLock {
            cache.clear()
        }
    }

    suspend fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cacheSize" to cache.size,
            "hitRate" to if (cacheHitCounter.count() + cacheMissCounter.count() > 0) {
                cacheHitCounter.count() / (cacheHitCounter.count() + cacheMissCounter.count())
            } else 0.0
        )
    }
}