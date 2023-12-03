package io.aethibo.features.users.domain.repository

import io.aethibo.features.users.domain.model.User

interface UsersRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findByUsername(username: String): User?
    suspend fun create(user: User): Long
    suspend fun update(email: String, user: User): User?
    suspend fun findIsFollowUser(email: String, userIdToFollow: Long): Boolean
    suspend fun follow(email: String, usernameToFollow: String): User
    suspend fun unfollow(email: String, usernameToUnFollow: String): User
}
