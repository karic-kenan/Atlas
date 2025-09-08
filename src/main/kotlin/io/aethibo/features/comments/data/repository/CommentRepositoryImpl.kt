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
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class CommentRepositoryImpl : CommentRepository {
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

    private suspend fun findById(commentId: Long): Comment? {
        return try {
            validateCommentId(commentId)

            dbQuery {
                CommentEntity.selectAll()
                    .where { CommentEntity.id eq commentId }
                    .map { it.toCommentDomain(null) }
                    .firstOrNull()
            }
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("findById", e)
        }
    }

    override suspend fun create(slugCommented: String, email: String, comment: Comment): Comment? {
        return try {
            validateSlug(slugCommented)
            validateEmail(email)
            validateCommentInput(comment)

            var user: User? = null
            val commentId = dbQuery {
                user = UserEntity.selectAll()
                    .where { UserEntity.email eq email }
                    .map { it.toUserDomain() }
                    .firstOrNull() ?: throw CommentException.AuthorNotFound(email)

                // Verify article exists (optional - depends on your business logic)
                val articleExists = ArticleEntity.selectAll()
                    .where { ArticleEntity.slug eq slugCommented }
                    .count() > 0

                if (!articleExists) throw CommentException.ArticleNotFound(slugCommented)

                CommentEntity.insertAndGetId { row ->
                    row[body] = comment.body
                    row[createdAt] = System.currentTimeMillis()
                    row[updatedAt] = System.currentTimeMillis()
                    row[slug] = slugCommented
                    row[author] = user.id!!
                }.value
            }

            findById(commentId)?.copy(author = user) ?: throw CommentException.CommentCreationFailed
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("add", e)
        }
    }

    override suspend fun find(slug: String): List<Comment> {
        return try {
            validateSlug(slug)

            val comments = dbQuery {
                CommentEntity.join(
                    UserEntity,
                    JoinType.INNER,
                    additionalConstraint = { CommentEntity.author eq UserEntity.id })
                    .selectAll()
                    .where { CommentEntity.slug eq slug }
                    .map { it.toCommentDomain(it.toUserDomain()) }
            }

            // You can choose whether to throw exception for empty results or return empty list
            // Based on your articles repo, it seems you throw for empty results:
            if (comments.isEmpty()) throw CommentException.CommentsNotFoundForSlug(slug)

            comments
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("findBySlug", e)
        }
    }

    override suspend fun delete(id: Long, slug: String) {
        try {
            validateCommentId(id)
            validateSlug(slug)

            // Verify comment exists
            findById(id) ?: throw CommentException.CommentNotFound(id)

            val deletedCount = dbQuery {
                CommentEntity.deleteWhere { CommentEntity.id eq id and (CommentEntity.slug eq slug) }
            }

            if (deletedCount == 0) {
                throw CommentException.CommentDeletionFailed
            }
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("delete", e)
        }
    }

    // Additional method you might want to add for authorization
    suspend fun deleteWithAuthorization(id: Long, slug: String, userId: Long) {
        try {
            validateCommentId(id)
            validateSlug(slug)

            // Verify comment exists and user owns it
            val comment = findById(id) ?: throw CommentException.CommentNotFound(id)

            if (comment.author?.id != userId) {
                throw CommentException.UnauthorizedCommentDeletion(id, userId)
            }

            val deletedCount = dbQuery {
                CommentEntity.deleteWhere {
                    CommentEntity.id eq id and (CommentEntity.slug eq slug) and (CommentEntity.author eq userId)
                }
            }

            if (deletedCount == 0) {
                throw CommentException.CommentDeletionFailed
            }
        } catch (e: CommentException) {
            throw e
        } catch (e: Exception) {
            throw CommentException.DatabaseError("deleteWithAuthorization", e)
        }
    }
}
