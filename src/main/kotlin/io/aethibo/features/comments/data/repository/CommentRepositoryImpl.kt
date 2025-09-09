package io.aethibo.features.comments.data.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.articles.data.model.ArticleEntity
import io.aethibo.features.articles.data.model.ArticleEntity.slug
import io.aethibo.features.comments.data.failure.CommentException
import io.aethibo.features.comments.data.model.CommentEntity
import io.aethibo.features.comments.domain.mapper.toCommentDomain
import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.comments.domain.repository.CommentRepository
import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.mapper.toUserDomain
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

    private suspend fun findById(commentId: Long): Comment? = try {
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

    override suspend fun create(slugCommented: String, email: String, comment: Comment): Comment? = try {
        validateSlug(slugCommented)
        validateEmail(email)
        validateCommentInput(comment)

        dbQuery {
            val user = UserEntity.selectAll()
                .where { UserEntity.email eq email }
                .map { it.toUserDomain() }
                .firstOrNull() ?: throw CommentException.AuthorNotFound(email)

            val articleExists = ArticleEntity.selectAll()
                .where { slug eq slugCommented }
                .count() > 0

            if (!articleExists) throw CommentException.ArticleNotFound(slugCommented)

            val commentId = CommentEntity.insertAndGetId { row ->
                row[body] = comment.body
                row[article] = slugCommented
                row[author] = user.id!!
            }.value

            findById(commentId)?.copy(author = user) ?: throw CommentException.CommentCreationFailed
        }
    } catch (e: CommentException) {
        throw e
    } catch (e: Exception) {
        throw CommentException.DatabaseError("create", e)
    }

    override suspend fun find(slug: String): List<Comment> = try {
        validateSlug(slug)
        dbQuery {
            val comments = CommentEntity.join(
                otherTable = UserEntity,
                joinType = JoinType.INNER,
                additionalConstraint = { CommentEntity.author eq UserEntity.id }
            )
                .selectAll()
                .where { CommentEntity.article eq slug }
                .map { it.toCommentDomain(it.toUserDomain()) }

            if (comments.isEmpty()) throw CommentException.CommentsNotFoundForSlug(slug)

            comments
        }
    } catch (e: CommentException) {
        throw e
    } catch (e: Exception) {
        throw CommentException.DatabaseError("findBySlug", e)
    }

    override suspend fun delete(id: Long, slug: String) {
        try {
            validateCommentId(id)
            validateSlug(slug)

            findById(id) ?: throw CommentException.CommentNotFound(id)

            val deletedCount = dbQuery {
                CommentEntity.deleteWhere { CommentEntity.id eq id and (CommentEntity.article eq slug) }
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
}
