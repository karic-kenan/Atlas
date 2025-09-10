package io.aethibo.features.comments.data.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.model.CommentEntity
import io.aethibo.features.comments.domain.mapper.toCommentDomain
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentRepository
import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.mapper.toUserDomain
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class CommentPagination(
    val limit: Int = 20,
    val offset: Long = 0,
    val sortOrder: CommentSortOrder = CommentSortOrder.NEWEST_FIRST
) {
    enum class CommentSortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST
    }

    fun validate() {
        if (limit !in 1..100) throw CommentException.InvalidLimit(limit)
        if (offset < 0) throw CommentException.InvalidOffset(offset)
    }
}

// Cursor-based pagination for better performance with large datasets
data class CursorPagination(
    val limit: Int = 20,
    val cursor: Long? = null, // Comment ID for cursor
    val sortOrder: CommentPagination.CommentSortOrder = CommentPagination.CommentSortOrder.NEWEST_FIRST
) {
    fun validate() {
        if (limit !in 1..100) throw CommentException.InvalidLimit(limit)
        if (cursor != null && cursor <= 0) throw CommentException.InvalidParameter("Invalid cursor")
    }
}

data class CommentStats(
    val totalComments: Long,
    val commentsToday: Long,
    val activeCommenters: Long,
    val averageCommentLength: Double
)

data class CommentWithReplies(
    val comment: Comment,
    val replyCount: Long = 0,
    val isEdited: Boolean = false
)

data class PaginatedComments(
    val comments: List<Comment>,
    val nextCursor: Long?,
    val hasMore: Boolean,
    val totalCount: Long? = null
)

class CommentRepositoryImpl(
    private val meterRegistry: MeterRegistry
) : CommentRepository {

    // --- Performance Monitoring (Fixed) ---
    private val dbTimer = Timer.builder("comments.database.query.time")
        .description("Time spent executing comment database queries")
        .register(meterRegistry)

    private val cacheHitCounter = Counter.builder("comments.cache.hits")
        .description("Number of comment cache hits")
        .register(meterRegistry)

    private val cacheMissCounter = Counter.builder("comments.cache.misses")
        .description("Number of comment cache misses")
        .register(meterRegistry)

    private val queryCounter = Counter.builder("comments.database.queries")
        .description("Total number of comment database queries executed")
        .register(meterRegistry)

    private val commentOperationCounter = Counter.builder("comments.operations")
        .tag("operation", "unknown") // Fixed: Added default tag
        .description("Total comment operations")
        .register(meterRegistry)

    // --- Caching Infrastructure ---
    private data class CacheKey(
        val type: String,
        val params: String
    )

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Instant,
        val ttl: Long = 300_000 // 5 minutes default TTL for comments
    ) {
        fun isExpired(): Boolean =
            Instant.now().toEpochMilli() - timestamp.toEpochMilli() > ttl
    }

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry<*>>()
    private val cacheMutex = Mutex()

    private suspend fun <T> withCache(
        cacheKey: CacheKey,
        ttl: Long = 300_000, // 5 minutes default
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

    // --- Enhanced Database Query Wrapper with Proper Logging ---
    private suspend fun <T> timedDbQuery(
        queryName: String,
        block: suspend () -> T
    ): T {
        queryCounter.increment()

        val startTime = System.currentTimeMillis()
        return try {
            val result = dbQuery { block() }
            val duration = System.currentTimeMillis() - startTime

            // Record timing
            dbTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

            // Log slow queries (> 200ms for comment operations)
            if (duration > 200) {
                println("SLOW COMMENT QUERY DETECTED: $queryName took ${duration}ms")
            }

            // Log all queries in debug mode
            if (duration > 50) { // Log queries taking more than 50ms
                println("COMMENT QUERY: $queryName completed in ${duration}ms")
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            dbTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)
            println("COMMENT QUERY FAILED: $queryName failed after ${duration}ms - ${e.message}")
            throw e
        }
    }

    // --- Validation helpers ---
    private fun validateCommentInput(comment: Comment) {
        if (comment.body.isBlank()) throw CommentException.EmptyCommentBody
    }

    private fun validateEmail(email: String) {
        if (email.isBlank() || !email.contains("@")) throw CommentException.InvalidAuthorEmail(email)
    }

    private fun validateSlug(slug: String) {
        if (slug.isBlank()) throw CommentException.InvalidSlug(slug)
    }

    private fun validateCommentId(commentId: Long) {
        if (commentId <= 0) throw CommentException.InvalidCommentId(commentId)
    }

    // --- Enhanced Core Operations ---
    private suspend fun findById(commentId: Long): Comment? = try {
        validateCommentId(commentId)

        val cacheKey = CacheKey("comment_by_id", commentId.toString())
        withCache(cacheKey) {
            timedDbQuery("find_comment_by_id") {
                CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.id eq commentId }
                    .map { it.toCommentDomain(it.toUserDomain()) }
                    .singleOrNull()
            }
        }
    } catch (e: CommentException) {
        throw e
    } catch (e: Exception) {
        throw CommentException.DatabaseError("findById", e)
    }

    suspend fun findByIdWithAuthor(commentId: Long): Comment? {
        return findById(commentId) // Already includes author due to JOIN
    }

    suspend fun findCommentsWithStats(slug: String): Pair<List<Comment>, CommentStats> {
        validateSlug(slug)

        val cacheKey = CacheKey("comments_with_stats", slug)
        return withCache(cacheKey, ttl = 180_000) { // 3 minutes cache for stats
            timedDbQuery("find_comments_with_stats") {
                // Get comments with single query using JOIN to avoid N+1
                val comments = CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.article eq slug }
                    .orderBy(CommentEntity.createdAt, SortOrder.DESC)
                    .map { it.toCommentDomain(it.toUserDomain()) }

                // Calculate stats
                val totalComments = comments.size.toLong()
                val today = Instant.now().minusSeconds(86400) // 24 hours ago
                val todayDateTime = LocalDateTime.ofInstant(today, java.time.ZoneOffset.UTC)

                val commentsToday = comments.count {
                    it.createdAt?.isAfter(todayDateTime) == true
                }.toLong()

                val activeCommenters = comments.mapNotNull { it.author?.id }.distinct().size.toLong()

                val averageLength = if (comments.isNotEmpty()) {
                    comments.map { it.body.length }.average()
                } else 0.0

                val stats = CommentStats(
                    totalComments = totalComments,
                    commentsToday = commentsToday,
                    activeCommenters = activeCommenters,
                    averageCommentLength = averageLength
                )

                comments to stats
            }
        }
    }

    override suspend fun create(slugCommented: String, email: String, comment: Comment): Comment? = try {
        validateSlug(slugCommented)
        validateEmail(email)
        validateCommentInput(comment)

        // Fixed: Use tag instead of second parameter
        Counter.builder("comments.operations.create")
            .register(meterRegistry)
            .increment()

        timedDbQuery("create_comment") {
            // Batch validation: check user exists and article exists in single query each
            val user = UserEntity.selectAll()
                .where { UserEntity.email eq email }
                .map { it.toUserDomain() }
                .singleOrNull() ?: throw CommentException.AuthorNotFound(email)

            val articleExists = ArticleEntity.selectAll()
                .where { ArticleEntity.slug eq slugCommented }
                .count() > 0

            if (!articleExists) throw CommentException.ArticleNotFound(slugCommented)

            val commentId = CommentEntity.insertAndGetId { row ->
                row[CommentEntity.body] = comment.body
                row[CommentEntity.article] = slugCommented
                row[CommentEntity.author] = user.id!!
            }.value

            // Invalidate relevant caches
            invalidateCache("comments")
            invalidateCache(slugCommented)

            // Return the created comment with author information
            comment.copy(
                id = commentId,
                author = user,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    } catch (e: CommentException) {
        throw e
    } catch (e: Exception) {
        throw CommentException.DatabaseError("create", e)
    }

    override suspend fun find(slug: String): List<Comment> = try {
        validateSlug(slug)

        val cacheKey = CacheKey("comments_by_slug", slug)
        withCache(cacheKey) {
            timedDbQuery("find_comments_by_slug") {
                // Single JOIN query to avoid N+1 problem
                val comments = CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.article eq slug }
                    .orderBy(CommentEntity.createdAt, SortOrder.DESC)
                    .map { it.toCommentDomain(it.toUserDomain()) }

                if (comments.isEmpty()) throw CommentException.CommentsNotFoundForSlug(slug)

                comments
            }
        }
    } catch (e: CommentException) {
        throw e
    } catch (e: Exception) {
        throw CommentException.DatabaseError("findBySlug", e)
    }

    // Improved offset-based pagination
    suspend fun findWithPagination(
        slug: String,
        pagination: CommentPagination
    ): PaginatedComments {
        validateSlug(slug)
        pagination.validate()

        val cacheKey =
            CacheKey("comments_paginated", "$slug:${pagination.limit}:${pagination.offset}:${pagination.sortOrder}")
        return withCache(cacheKey) {
            timedDbQuery("find_comments_paginated") {
                val sortOrder = when (pagination.sortOrder) {
                    CommentPagination.CommentSortOrder.NEWEST_FIRST -> SortOrder.DESC
                    CommentPagination.CommentSortOrder.OLDEST_FIRST -> SortOrder.ASC
                }

                // Get total count and comments in separate optimized queries
                val totalCount = CommentEntity.selectAll().where { CommentEntity.article eq slug }.count()

                val comments = CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.article eq slug }
                    .orderBy(CommentEntity.createdAt, sortOrder)
                    .limit(pagination.limit)
                    .offset(pagination.offset)
                    .map { it.toCommentDomain(it.toUserDomain()) }

                val hasMore = pagination.offset + pagination.limit < totalCount

                PaginatedComments(
                    comments = comments,
                    nextCursor = if (comments.isNotEmpty() && hasMore) comments.last().id else null,
                    hasMore = hasMore,
                    totalCount = totalCount
                )
            }
        }
    }

    // Optimized cursor-based pagination for better performance
    suspend fun findWithCursorPagination(
        slug: String,
        pagination: CursorPagination
    ): PaginatedComments {
        validateSlug(slug)
        pagination.validate()

        val cacheKey = CacheKey(
            "comments_cursor_paginated",
            "$slug:${pagination.limit}:${pagination.cursor}:${pagination.sortOrder}"
        )
        return withCache(cacheKey) {
            timedDbQuery("find_comments_cursor_paginated") {
                val baseQuery = CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll().where { CommentEntity.article eq slug }

                val query = if (pagination.cursor != null) {
                    when (pagination.sortOrder) {
                        CommentPagination.CommentSortOrder.NEWEST_FIRST ->
                            baseQuery.andWhere { CommentEntity.id less pagination.cursor }

                        CommentPagination.CommentSortOrder.OLDEST_FIRST ->
                            baseQuery.andWhere { CommentEntity.id greater pagination.cursor }
                    }
                } else {
                    baseQuery
                }

                val sortOrder = when (pagination.sortOrder) {
                    CommentPagination.CommentSortOrder.NEWEST_FIRST -> SortOrder.DESC
                    CommentPagination.CommentSortOrder.OLDEST_FIRST -> SortOrder.ASC
                }

                val comments = query
                    .orderBy(CommentEntity.id, sortOrder)
                    .limit(pagination.limit + 1) // Get one extra to determine hasMore
                    .map { it.toCommentDomain(it.toUserDomain()) }

                val hasMore = comments.size > pagination.limit
                val actualComments = if (hasMore) comments.dropLast(1) else comments

                PaginatedComments(
                    comments = actualComments,
                    nextCursor = if (hasMore && actualComments.isNotEmpty()) actualComments.last().id else null,
                    hasMore = hasMore
                )
            }
        }
    }

    suspend fun findByAuthor(
        email: String,
        limit: Int = 20,
        offset: Long = 0
    ): List<Comment> {
        validateEmail(email)
        if (limit !in 1..100) throw CommentException.InvalidLimit(limit)
        if (offset < 0) throw CommentException.InvalidOffset(offset)

        val cacheKey = CacheKey("comments_by_author", "$email:$limit:$offset")
        return withCache(cacheKey) {
            timedDbQuery("find_comments_by_author") {
                CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { UserEntity.email eq email }
                    .orderBy(CommentEntity.createdAt, SortOrder.DESC)
                    .limit(limit)
                    .offset(offset)
                    .map { it.toCommentDomain(it.toUserDomain()) }
            }
        }
    }

    suspend fun findRecentComments(
        hoursBack: Int = 24,
        limit: Int = 50
    ): List<Comment> {
        if (limit !in 1..200) throw CommentException.InvalidLimit(limit)
        if (hoursBack !in 1..168) throw CommentException.InvalidParameter("Hours back must be between 1 and 168")

        val cacheKey = CacheKey("recent_comments", "$hoursBack:$limit")
        return withCache(cacheKey, ttl = 300_000) { // 5 minutes cache
            timedDbQuery("find_recent_comments") {
                val cutoffTime = Instant.now().minusSeconds(hoursBack * 3600L)
                val cutoffDateTime = LocalDateTime.ofInstant(cutoffTime, java.time.ZoneOffset.UTC)

                CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.createdAt greaterEq cutoffDateTime }
                    .orderBy(CommentEntity.createdAt, SortOrder.DESC)
                    .limit(limit)
                    .map { it.toCommentDomain(it.toUserDomain()) }
            }
        }
    }

    override suspend fun delete(id: Long, slug: String) {
        try {
            validateCommentId(id)
            validateSlug(slug)

            Counter.builder("comments.operations.delete")
                .register(meterRegistry)
                .increment()

            // Optimized: Check existence and delete in single transaction
            val result = timedDbQuery("delete_comment") {
                val commentExists = CommentEntity.selectAll()
                    .where { (CommentEntity.id eq id) and (CommentEntity.article eq slug) }
                    .count() > 0

                if (!commentExists) throw CommentException.CommentNotFound(id)

                CommentEntity.deleteWhere {
                    (CommentEntity.id eq id) and (CommentEntity.article eq slug)
                }
            }

            if (result == 0) {
                throw CommentException.CommentDeletionFailed
            }

            // Invalidate relevant caches
            invalidateCache("comments")
            invalidateCache(slug)
            invalidateCache(id.toString())
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("delete", e)
        }
    }

    suspend fun update(id: Long, slug: String, newBody: String): Comment? {
        try {
            validateCommentId(id)
            validateSlug(slug)
            if (newBody.isBlank()) throw CommentException.EmptyCommentBody

            Counter.builder("comments.operations.update")
                .register(meterRegistry)
                .increment()

            val updatedComment = timedDbQuery("update_comment") {
                // Check if comment exists and belongs to the article
                val existingComment = CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { (CommentEntity.id eq id) and (CommentEntity.article eq slug) }
                    .map { it.toCommentDomain(it.toUserDomain()) }
                    .singleOrNull() ?: throw CommentException.CommentNotFound(id)

                // Update the comment
                val updateCount = CommentEntity.update(
                    where = {
                        (CommentEntity.id eq id) and (CommentEntity.article eq slug)
                    }
                ) { row ->
                    row[CommentEntity.body] = newBody
                    row[CommentEntity.updatedAt] = LocalDateTime.now()
                }

                if (updateCount == 0) throw CommentException.CommentUpdateFailed(id)

                existingComment.copy(
                    body = newBody,
                    updatedAt = LocalDateTime.now()
                )
            }

            // Invalidate relevant caches
            invalidateCache("comments")
            invalidateCache(slug)
            invalidateCache(id.toString())

            return updatedComment
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("update", e)
        }
    }

    // --- Bulk Operations (Fixed) ---
    suspend fun findCommentsByIds(commentIds: List<Long>): Map<Long, Comment> {
        if (commentIds.isEmpty()) return emptyMap()

        val cacheKey = CacheKey("comments_by_ids", commentIds.sorted().joinToString(","))
        return withCache(cacheKey) {
            timedDbQuery("find_comments_by_ids") {
                CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id }
                ).selectAll()
                    .where { CommentEntity.id inList commentIds }
                    .associate {
                        it[CommentEntity.id].value to it.toCommentDomain(it.toUserDomain())
                    }
            }
        }
    }

    suspend fun deleteCommentsByArticle(slug: String): Long {
        validateSlug(slug)

        Counter.builder("comments.operations.bulk_delete")
            .register(meterRegistry)
            .increment()

        val deletedCount = timedDbQuery("delete_comments_by_article") {
            CommentEntity.deleteWhere { CommentEntity.article eq slug }
        }

        // Invalidate relevant caches
        invalidateCache("comments")
        invalidateCache(slug)

        return deletedCount.toLong()
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
            } else 0.0,
            "totalHits" to cacheHitCounter.count(),
            "totalMisses" to cacheMissCounter.count()
        )
    }

    // --- Health Check ---
    suspend fun getHealthStats(): Map<String, Any> {
        val healthCheckTime = Instant.now().minusSeconds(86400)
        val healthCheckDateTime = LocalDateTime.ofInstant(healthCheckTime, java.time.ZoneOffset.UTC)

        return timedDbQuery("health_check") {
            val totalComments = CommentEntity.selectAll().count()
            val commentsToday = CommentEntity.selectAll()
                .where { CommentEntity.createdAt greaterEq healthCheckDateTime }
                .count()

            mapOf(
                "totalComments" to totalComments,
                "commentsToday" to commentsToday,
                "averageQueryTime" to "${dbTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)}ms",
                "cacheStats" to getCacheStats()
            )
        }
    }
}
