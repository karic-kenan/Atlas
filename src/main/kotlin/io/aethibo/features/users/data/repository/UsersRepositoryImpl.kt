package io.aethibo.features.users.data.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.data.model.FollowsEntity
import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.mapper.toUserDomain
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*

class UsersRepositoryImpl : UsersRepository {
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

    override suspend fun findById(id: Long): User? {
        return try {
            dbQuery {
                UserEntity.selectAll()
                    .where { UserEntity.id eq id }
                    .map { it.toUserDomain() }
                    .firstOrNull()
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findByEmail", e)
        }
    }

    override suspend fun findByEmail(email: String): User? {
        return try {
            validateEmail(email)

            dbQuery {
                UserEntity.selectAll()
                    .where { UserEntity.email eq email }
                    .limit(1)
                    .map { it.toUserDomain() }
                    .firstOrNull()
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

            dbQuery {
                UserEntity.selectAll()
                    .where { UserEntity.username eq username }
                    .map { it.toUserDomain() }
                    .firstOrNull()
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findByUsername", e)
        }
    }

    override suspend fun create(user: User): Long {
        return try {
            validateUserForCreation(user)

            // Check if user already exists
            findByEmail(user.email)?.let {
                throw UserException.UserAlreadyExists(user.email)
            }

            findByUsername(user.username!!)?.let {
                throw UserException.UserAlreadyExists(user.username)
            }

            dbQuery {
                UserEntity.insertAndGetId { row ->
                    row[email] = user.email
                    row[username] = user.username
                    row[password] = user.password!!
                    row[bio] = user.bio
                    row[image] = user.image
                }.value
            }
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

            // Verify user exists
            findByEmail(email) ?: throw UserException.UserNotFoundByEmail(email)

            // If username is being updated, check it's not taken by another user
            user.username?.let { newUsername ->
                findByUsername(newUsername)?.let { existingUser ->
                    if (existingUser.email != email) {
                        throw UserException.UserAlreadyExists(newUsername)
                    }
                }
            }

            dbQuery {
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

            dbQuery {
                UserEntity.join(
                    otherTable = FollowsEntity,
                    joinType = JoinType.INNER,
                    additionalConstraint = {
                        FollowsEntity.user eq UserEntity.id and (FollowsEntity.follower eq userIdToFollow)
                    }
                ).select(UserEntity.id)
                    .where { UserEntity.email eq email }
                    .count() > 0
            }
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("findIsFollowUser", e)
        }
    }

    override suspend fun follow(email: String, usernameToFollow: String): User {
        return try {
            validateEmail(email)
            validateUsername(usernameToFollow)

            val user = findByEmail(email) ?: throw UserException.UserNotFoundByEmail(email)
            val userToFollow =
                findByUsername(usernameToFollow) ?: throw UserException.UserNotFoundByUsername(usernameToFollow)

            // Prevent self-following
            if (user.email == userToFollow.email) {
                throw UserException.SelfFollowAttempt(email)
            }

            // Check if already following
            if (findIsFollowUser(email, userToFollow.id!!)) {
                throw UserException.AlreadyFollowing(email, usernameToFollow)
            }

            dbQuery {
                FollowsEntity.insertIgnore { row ->
                    row[FollowsEntity.user] = userToFollow.id
                    row[follower] = user.id!!
                }
            }

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

            val user = findByEmail(email) ?: throw UserException.UserNotFoundByEmail(email)
            val userToUnfollow =
                findByUsername(usernameToUnFollow) ?: throw UserException.UserNotFoundByUsername(usernameToUnFollow)

            // Prevent self-unfollowing (though this should never happen)
            if (user.email == userToUnfollow.email) {
                throw UserException.SelfFollowAttempt(email)
            }

            // Check if actually following
            if (!findIsFollowUser(email, userToUnfollow.id!!)) {
                throw UserException.NotFollowing(email, usernameToUnFollow)
            }

            val deletedCount = dbQuery {
                FollowsEntity.deleteWhere {
                    FollowsEntity.user eq userToUnfollow.id and (follower eq user.id!!)
                }
            }

            if (deletedCount == 0) {
                throw UserException.UnfollowOperationFailed(email, usernameToUnFollow)
            }

            userToUnfollow
        } catch (e: UserException) {
            throw e
        } catch (e: Exception) {
            throw UserException.DatabaseError("unfollow", e)
        }
    }
}
