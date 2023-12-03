package io.aethibo.features.users.domain.service

import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.model.User

interface UsersService {
    suspend fun create(user: User): User
    suspend fun authenticate(user: User): User
    suspend fun getByEmail(email: String): User
    suspend fun update(email: String?, user: User): User?
    suspend fun getProfileByUsername(email: String, usernameFollowing: String?): Profile
    suspend fun follow(email: String, usernameToFollow: String): Profile
    suspend fun unfollow(email: String, usernameToUnfollow: String): Profile
}
