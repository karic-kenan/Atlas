package io.aethibo.features.comments.data.repository

import io.aethibo.core.config.DatabaseFactory.dbQuery
import io.aethibo.core.exceptions.BadRequestResponse
import io.aethibo.features.comments.data.table.Comments
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentsRepository
import io.aethibo.features.users.data.table.Users
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class CommentsRepositoryImpl : CommentsRepository {
    private suspend fun findById(commentId: Long): Comment? {
        return dbQuery {
            Comments.selectAll()
                .where { Comments.id eq commentId }
                .map { Comments.toDomain(it, null) }
                .firstOrNull()
        }
    }

    override suspend fun add(slugCommented: String, email: String, comment: Comment): Comment? {
        var user: User? = null
        return dbQuery {
            user = Users.selectAll()
                .where { Users.email eq email }
                .map { Users.toDomain(it) }.firstOrNull() ?: throw BadRequestResponse()
            Comments.insertAndGetId { row ->
                row[body] = comment.body
                row[createdAt] = System.currentTimeMillis()
                row[updatedAt] = System.currentTimeMillis()
                row[slug] = slugCommented
                row[author] = user?.id!!
            }.value
        }.let { commentId ->
            findById(commentId)?.copy(author = user)
        }
    }

    override suspend fun findBySlug(slug: String): List<Comment> = dbQuery {
        Comments.join(Users, JoinType.INNER, additionalConstraint = { Comments.author eq Users.id })
            .selectAll()
            .where { Comments.slug eq slug }
            .map { Comments.toDomain(it, Users.toDomain(it)) }
    }

    override suspend fun delete(id: Long, slug: String) {
        dbQuery {
            Comments.deleteWhere { Comments.id eq id and (Comments.slug eq slug) }
        }
    }
}
