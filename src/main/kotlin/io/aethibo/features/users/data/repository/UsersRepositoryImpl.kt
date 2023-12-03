package io.aethibo.features.users.data.repository

import io.aethibo.core.config.DatabaseFactory.dbQuery
import io.aethibo.core.exceptions.NotFoundResponse
import io.aethibo.features.users.data.table.Follows
import io.aethibo.features.users.data.table.Users
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.domain.repository.UsersRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class UsersRepositoryImpl : UsersRepository {
    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .map { Users.toDomain(it) }
            .firstOrNull()
    }

    override suspend fun findByUsername(username: String): User? = dbQuery {
        Users.select { Users.username eq username }
            .map { Users.toDomain(it) }
            .firstOrNull()
    }

    override suspend fun create(user: User): Long = dbQuery {
        Users.insertAndGetId { row ->
            row[email] = user.email
            row[username] = user.username!!
            row[password] = user.password!!
            row[bio] = user.bio
            row[image] = user.image
        }.value
    }

    override suspend fun update(email: String, user: User): User? {
        dbQuery {
            Users.update(where = { Users.email eq email }) { row ->
                row[Users.email] = user.email
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

        return findByEmail(user.email)
    }

    override suspend fun findIsFollowUser(email: String, userIdToFollow: Long): Boolean = dbQuery {
        Users.join(
            otherTable = Follows,
            joinType = JoinType.INNER,
            additionalConstraint = {
                Follows.user eq Users.id and (Follows.follower eq userIdToFollow)
            }
        ).select {
            Users.email eq email
        }.count() > 0
    }

    override suspend fun follow(email: String, usernameToFollow: String): User {
        val user = findByEmail(email) ?: throw NotFoundResponse()
        val userToFollow = findByUsername(usernameToFollow) ?: throw NotFoundResponse()

        dbQuery {
            Follows.insert { row ->
                row[Follows.user] = userToFollow.id!!
                row[follower] = user.id!!
            }
        }

        return userToFollow
    }

    override suspend fun unfollow(email: String, usernameToUnFollow: String): User {
        val user = findByEmail(email) ?: throw NotFoundResponse()
        val userToUnfollow = findByUsername(usernameToUnFollow) ?: throw NotFoundResponse()

        dbQuery {
            Follows.deleteWhere {
                Follows.user eq user.id!! and (follower eq userToUnfollow.id!!)
            }
        }

        return userToUnfollow
    }
}
