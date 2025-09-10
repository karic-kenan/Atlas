package io.aethibo.features.users.data.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.model.FollowsEntity
import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.mapper.toUserDomain
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class UserSearchCriteria(
    val query: String? = null,
    val minFollowers: Int? = null,
    val maxFollowers: Int? = null,
    val hasImage: Boolean? = null,
    val hasBio: Boolean? = null
)

data class FollowStats(
    val followersCount: Long,
    val followingCount: Long,
    val mutualFollowersCount: Long = 0
)

class UsersRepositoryImpl(
    private val meterRegistry: MeterRegistry
) : UsersRepository {

    // --- Performance Monitoring ---
    private val dbTimer = Timer.builder("users.database.query.time")
        .description("Time spent executing user database queries")
        .register(meterRegistry)

    private val cacheHitCounter = Counter.builder("users.cache.hits")
        .description("Number of user cache hits")
        .register(meterRegistry)

    private val cacheMissCounter = Counter.builder("users.cache.misses")
        .description("Number of user cache misses")
        .register(meterRegistry)

    private val queryCounter = Counter.builder("users.database.queries")
        .description("Total number of user database queries executed")
        .register(meterRegistry)

    private val followOperationCounter = Counter.builder("users.follow.operations")
        .description("Total follow/unfollow operations")
        .tag("operation", "follow")
        .register(meterRegistry)

    private val unfollowOperationCounter = Counter.builder("users.follow.operations")
        .description("Total follow/unfollow operations")
        .tag("operation", "unfollow")
        .register(meterRegistry)

    // --- Caching Infrastructure ---
    private data class CacheKey(
        val type: String,
        val params: String
    )

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Instant,
        val ttl: Long = 600_000 // 10 minutes default TTL for users
    ) {
        fun isExpired(): Boolean =
            Instant.now().toEpochMilli() - timestamp.toEpochMilli() > ttl
    }

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry<*>>()
    private val cacheMutex = Mutex()

    private suspend fun <T> withCache(
        cacheKey: CacheKey,
        ttl: Long = 600_000, // 10 minutes default
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

    // --- Enhanced Database Query Wrapper ---
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

            // Log slow queries (> 300ms for user operations)
            if (duration > 300) {
                println("SLOW USER QUERY DETECTED: $queryName took ${duration}ms")
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            dbTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)
            println("USER QUERY FAILED: $queryName failed after ${duration}ms - ${e.message}")
            throw e
        }
    }

    // --- Validation helpers ---
    private fun validateEmail(email: String) {
        if (email.isBlank() || !email.contains("@")) throw UserException.InvalidEmail(email)
    }

    private fun validateUsername(username: String?) {
        if (username.isNullOrBlank()) throw UserException.EmptyRequiredField("username")
        if (username.length !in 3..50) throw UserException.InvalidUsername(username)
    }

    private fun validatePassword(password: String?) {
        if (password.isNullOrBlank()) throw UserException.EmptyRequiredField("password")
        if (password.length < 6) throw UserException.InvalidPassword("Password must be at least 6 characters")
    }

    private fun validateUserForCreation(user: User) {
        validateEmail(user.email)
        validateUsername(user.username)
        validatePassword(user.password)
    }

    private fun validateUserForUpdate(user: User) {
        validateEmail(user.email)
        // Username and password are optional for updates, but if provided should be valid
        user.username?.let { validateUsername(it) }
        user.password?.let { validatePassword(it) }
    }

    // --- Optimized Core Operations ---
    override suspend fun findById(id: Long): User? {
        if (id <= 0) return null

        val cacheKey = CacheKey("user_by_id", id.toString())
        return withCache(cacheKey) {
            timedDbQuery("find_by_id") {
                UserEntity.selectAll()
                    .where { UserEntity.id eq id }
                    .map { it.toUserDomain() }
                    .firstOrNull()
            }
        }
    }

    override suspend fun findByEmail(email: String): User? {
        return try {
            validateEmail(email)

            val cacheKey = CacheKey("user_by_email", email)
            withCache(cacheKey) {
                timedDbQuery("find_by_email") {
                    UserEntity.selectAll()
                        .where { UserEntity.email eq email }
                        .limit(1)
                        .map { it.toUserDomain() }
                        .firstOrNull()
                }
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findByEmail", e)
        }
    }

    override suspend fun findByUsername(username: String): User? {
        return try {
            validateUsername(username)

            val cacheKey = CacheKey("user_by_username", username)
            withCache(cacheKey) {
                timedDbQuery("find_by_username") {
                    UserEntity.selectAll()
                        .where { UserEntity.username eq username }
                        .map { it.toUserDomain() }
                        .firstOrNull()
                }
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findByUsername", e)
        }
    }

    // --- Enhanced User Operations ---
    suspend fun findUserWithStats(username: String): Pair<User, FollowStats>? {
        validateUsername(username)

        val cacheKey = CacheKey("user_with_stats", username)
        return withCache(cacheKey, ttl = 300_000) { // 5 minutes cache for stats
            timedDbQuery("find_user_with_stats") {
                // Single query to get user and follower/following counts
                val userResult = UserEntity.selectAll()
                    .where { UserEntity.username eq username }
                    .map { it.toUserDomain() }
                    .firstOrNull() ?: return@timedDbQuery null

                // Get follow statistics in parallel queries
                val followersCount = FollowsEntity.selectAll()
                    .where { FollowsEntity.user eq userResult.id!! }
                    .count()

                val followingCount = FollowsEntity.selectAll()
                    .where { FollowsEntity.follower eq userResult.id }
                    .count()

                val stats = FollowStats(
                    followersCount = followersCount,
                    followingCount = followingCount
                )

                userResult to stats
            }
        }
    }

    suspend fun findUsersBySearch(
        criteria: UserSearchCriteria,
        limit: Int = 20,
        offset: Long = 0
    ): List<User> {
        if (limit !in 1..100) throw UserException.InvalidLimit(limit)
        if (offset < 0) throw UserException.InvalidOffset(offset)

        val cacheKey = CacheKey("users_search", "${criteria.hashCode()}:$limit:$offset")
        return withCache(cacheKey, ttl = 180_000) { // 3 minutes cache for searches
            timedDbQuery("find_users_by_search") {
                var query = UserEntity.selectAll()

                // Build dynamic WHERE conditions
                criteria.query?.let { searchQuery ->
                    if (searchQuery.isNotBlank()) {
                        query = query.andWhere {
                            (UserEntity.username like "%$searchQuery%") or
                                    (UserEntity.bio like "%$searchQuery%")
                        }
                    }
                }

                criteria.hasImage?.let { hasImage ->
                    query = if (hasImage) {
                        query.andWhere { UserEntity.image.isNotNull() }
                    } else {
                        query.andWhere { UserEntity.image.isNull() }
                    }
                }

                criteria.hasBio?.let { hasBio ->
                    query = if (hasBio) {
                        query.andWhere { UserEntity.bio.isNotNull() }
                    } else {
                        query.andWhere { UserEntity.bio.isNull() }
                    }
                }

                query.limit(limit)
                    .offset(offset)
                    .orderBy(UserEntity.username, SortOrder.ASC)
                    .map { it.toUserDomain() }
            }
        }
    }

    override suspend fun create(user: User): Long {
        return try {
            validateUserForCreation(user)

            // Batch check for existing users (single query)
            val existingUsers = timedDbQuery("check_existing_users") {
                UserEntity.selectAll()
                    .where {
                        (UserEntity.email eq user.email) or
                                (UserEntity.username eq user.username!!)
                    }
                    .map { it.toUserDomain() }
            }

            existingUsers.find { it.email == user.email }?.let {
                throw UserException.UserAlreadyExists(user.email)
            }

            existingUsers.find { it.username == user.username }?.let {
                throw UserException.UserAlreadyExists(user.username!!)
            }

            val userId = timedDbQuery("create_user") {
                UserEntity.insertAndGetId { row ->
                    row[email] = user.email
                    row[username] = user.username!!
                    row[password] = user.password!!
                    row[bio] = user.bio
                    row[image] = user.image
                }.value
            }

            // Invalidate relevant caches
            invalidateCache("user")

            userId
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("create", e)
        }
    }

    override suspend fun update(email: String, user: User): User? {
        return try {
            validateEmail(email)
            validateUserForUpdate(user)

            // Verify user exists and get current data
            val existingUser = findByEmail(email) ?: throw UserException.UserNotFoundByEmail(email)

            // If username is being updated, check it's not taken by another user
            user.username?.let { newUsername ->
                if (newUsername != existingUser.username) {
                    findByUsername(newUsername)?.let { conflictUser ->
                        if (conflictUser.email != email) {
                            throw UserException.UserAlreadyExists(newUsername)
                        }
                    }
                }
            }

            timedDbQuery("update_user") {
                UserEntity.update(where = { UserEntity.email eq email }) { row ->
                    row[UserEntity.email] = user.email
                    if (user.username != null) {
                        row[username] = user.username
                    }
                    if (user.password != null) {
                        row[password] = user.password
                    }
                    if (user.bio != null) {
                        row[bio] = user.bio
                    }
                    if (user.image != null) {
                        row[image] = user.image
                    }
                }
            }

            // Invalidate caches for old and new data
            invalidateCache("user")
            invalidateCache(email)
            invalidateCache(existingUser.username!!)
            user.username?.let { invalidateCache(it) }

            findByEmail(user.email) ?: throw UserException.UserUpdateFailed(user.email)
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("update", e)
        }
    }

    override suspend fun findIsFollowUser(email: String, userIdToFollow: Long): Boolean {
        return try {
            validateEmail(email)
            if (userIdToFollow <= 0) throw UserException.UserNotFound(userIdToFollow.toString())

            val cacheKey = CacheKey("is_following", "$email:$userIdToFollow")
            withCache(cacheKey, ttl = 300_000) { // 5 minutes cache for follow status
                timedDbQuery("find_is_follow_user") {
                    FollowsEntity.join(
                        UserEntity,
                        JoinType.INNER,
                        additionalConstraint = {
                            (FollowsEntity.follower eq UserEntity.id) and
                                    (FollowsEntity.user eq userIdToFollow)
                        }
                    ).selectAll()
                        .where { UserEntity.email eq email }
                        .count() > 0
                }
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findIsFollowUser", e)
        }
    }

    suspend fun getFollowersWithPagination(
        username: String,
        limit: Int = 20,
        offset: Long = 0
    ): List<User> {
        validateUsername(username)
        if (limit !in 1..100) throw UserException.InvalidLimit(limit)
        if (offset < 0) throw UserException.InvalidOffset(offset)

        val cacheKey = CacheKey("followers", "$username:$limit:$offset")
        return withCache(cacheKey, ttl = 300_000) {
            timedDbQuery("get_followers_paginated") {
                // Single optimized query with JOIN
                UserEntity.alias("target").join(
                    FollowsEntity,
                    JoinType.INNER,
                    additionalConstraint = { UserEntity.alias("target")[UserEntity.id] eq FollowsEntity.user }
                ).join(
                    UserEntity.alias("follower"),
                    JoinType.INNER,
                    additionalConstraint = { FollowsEntity.follower eq UserEntity.alias("follower")[UserEntity.id] }
                ).selectAll()
                    .where { UserEntity.alias("target")[UserEntity.username] eq username }
                    .limit(limit)
                    .offset(offset)
                    .orderBy(UserEntity.alias("follower")[UserEntity.username], SortOrder.ASC)
                    .map {
                        User(
                            id = it[UserEntity.alias("follower")[UserEntity.id]].value,
                            email = it[UserEntity.alias("follower")[UserEntity.email]],
                            username = it[UserEntity.alias("follower")[UserEntity.username]],
                            bio = it[UserEntity.alias("follower")[UserEntity.bio]],
                            image = it[UserEntity.alias("follower")[UserEntity.image]]
                        )
                    }
            }
        }
    }

    suspend fun getFollowingWithPagination(
        username: String,
        limit: Int = 20,
        offset: Long = 0
    ): List<User> {
        validateUsername(username)
        if (limit !in 1..100) throw UserException.InvalidLimit(limit)
        if (offset < 0) throw UserException.InvalidOffset(offset)

        val cacheKey = CacheKey("following", "$username:$limit:$offset")
        return withCache(cacheKey, ttl = 300_000) {
            timedDbQuery("get_following_paginated") {
                // Single optimized query with JOIN
                UserEntity.alias("follower").join(
                    FollowsEntity,
                    JoinType.INNER,
                    additionalConstraint = { UserEntity.alias("follower")[UserEntity.id] eq FollowsEntity.follower }
                ).join(
                    UserEntity.alias("target"),
                    JoinType.INNER,
                    additionalConstraint = { FollowsEntity.user eq UserEntity.alias("target")[UserEntity.id] }
                ).selectAll()
                    .where { UserEntity.alias("follower")[UserEntity.username] eq username }
                    .limit(limit)
                    .offset(offset)
                    .orderBy(UserEntity.alias("target")[UserEntity.username], SortOrder.ASC)
                    .map {
                        User(
                            id = it[UserEntity.alias("target")[UserEntity.id]].value,
                            email = it[UserEntity.alias("target")[UserEntity.email]],
                            username = it[UserEntity.alias("target")[UserEntity.username]],
                            bio = it[UserEntity.alias("target")[UserEntity.bio]],
                            image = it[UserEntity.alias("target")[UserEntity.image]]
                        )
                    }
            }
        }
    }

    override suspend fun follow(email: String, usernameToFollow: String): User {
        return try {
            validateEmail(email)
            validateUsername(usernameToFollow)

            followOperationCounter.increment()

            // Batch fetch both users in single query
            val users = timedDbQuery("fetch_users_for_follow") {
                UserEntity.selectAll()
                    .where {
                        (UserEntity.email eq email) or
                                (UserEntity.username eq usernameToFollow)
                    }
                    .map { it.toUserDomain() }
            }

            val user = users.find { it.email == email }
                ?: throw UserException.UserNotFoundByEmail(email)
            val userToFollow = users.find { it.username == usernameToFollow }
                ?: throw UserException.UserNotFoundByUsername(usernameToFollow)

            // Prevent self-following
            if (user.email == userToFollow.email) {
                throw UserException.SelfFollowAttempt(email)
            }

            // Check if already following
            if (findIsFollowUser(email, userToFollow.id!!)) {
                throw UserException.AlreadyFollowing(email, usernameToFollow)
            }

            timedDbQuery("insert_follow") {
                FollowsEntity.insertIgnore { row ->
                    row[FollowsEntity.user] = userToFollow.id
                    row[follower] = user.id!!
                }
            }

            // Invalidate follow-related caches
            invalidateCache("is_following")
            invalidateCache("followers")
            invalidateCache("following")
            invalidateCache("user_with_stats")

            userToFollow
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("follow", e)
        }
    }

    override suspend fun unfollow(email: String, usernameToUnFollow: String): User {
        return try {
            validateEmail(email)
            validateUsername(usernameToUnFollow)

            unfollowOperationCounter.increment()

            // Batch fetch both users in single query
            val users = timedDbQuery("fetch_users_for_unfollow") {
                UserEntity.selectAll()
                    .where {
                        (UserEntity.email eq email) or
                                (UserEntity.username eq usernameToUnFollow)
                    }
                    .map { it.toUserDomain() }
            }

            val user = users.find { it.email == email }
                ?: throw UserException.UserNotFoundByEmail(email)
            val userToUnfollow = users.find { it.username == usernameToUnFollow }
                ?: throw UserException.UserNotFoundByUsername(usernameToUnFollow)

            // Prevent self-unfollowing
            if (user.email == userToUnfollow.email) {
                throw UserException.SelfFollowAttempt(email)
            }

            // Check if actually following
            if (!findIsFollowUser(email, userToUnfollow.id!!)) {
                throw UserException.NotFollowing(email, usernameToUnFollow)
            }

            val deletedCount = timedDbQuery("delete_follow") {
                FollowsEntity.deleteWhere {
                    FollowsEntity.user eq userToUnfollow.id and (follower eq user.id!!)
                }
            }

            if (deletedCount == 0) {
                throw UserException.UnfollowOperationFailed(email, usernameToUnFollow)
            }

            // Invalidate follow-related caches
            invalidateCache("is_following")
            invalidateCache("followers")
            invalidateCache("following")
            invalidateCache("user_with_stats")

            userToUnfollow
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("unfollow", e)
        }
    }

    // --- Bulk Operations for Performance ---
    suspend fun findUsersByIds(userIds: List<Long>): Map<Long, User> {
        if (userIds.isEmpty()) return emptyMap()

        val cacheKey = CacheKey("users_by_ids", userIds.sorted().joinToString(","))
        return withCache(cacheKey) {
            timedDbQuery("find_users_by_ids") {
                UserEntity.selectAll()
                    .where { UserEntity.id inList userIds }
                    .associate {
                        it[UserEntity.id].value to it.toUserDomain()
                    }
            }
        }
    }

    suspend fun findUsersFollowStatus(
        currentUserEmail: String,
        targetUsernames: List<String>
    ): Map<String, Boolean> {
        if (targetUsernames.isEmpty()) return emptyMap()

        val cacheKey = CacheKey("follow_status_bulk", "$currentUserEmail:${targetUsernames.sorted().joinToString(",")}")
        return withCache(cacheKey, ttl = 300_000) {
            timedDbQuery("find_users_follow_status_bulk") {
                // Get current user first
                val currentUser = findByEmail(currentUserEmail)
                    ?: return@timedDbQuery emptyMap<String, Boolean>()

                // Single query to get all follow statuses
                val followedUsernames = FollowsEntity.join(
                    UserEntity.alias("follower"),
                    JoinType.INNER,
                    additionalConstraint = { FollowsEntity.follower eq UserEntity.alias("follower")[UserEntity.id] }
                ).join(
                    UserEntity.alias("target"),
                    JoinType.INNER,
                    additionalConstraint = { FollowsEntity.user eq UserEntity.alias("target")[UserEntity.id] }
                ).selectAll()
                    .where {
                        (UserEntity.alias("follower")[UserEntity.id] eq currentUser.id!!) and
                                (UserEntity.alias("target")[UserEntity.username] inList targetUsernames)
                    }
                    .map { it[UserEntity.alias("target")[UserEntity.username]] }
                    .toSet()

                targetUsernames.associateWith { it in followedUsernames }
            }
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
            } else 0.0,
            "followOperations" to followOperationCounter.count(),
            "unfollowOperations" to unfollowOperationCounter.count()
        )
    }

    // --- Performance Analytics ---
    suspend fun getSlowQueryReport(): Map<String, Long> {
        // This would be implemented based on your logging infrastructure
        // For now, return empty map as placeholder
        return emptyMap()
    }
}
