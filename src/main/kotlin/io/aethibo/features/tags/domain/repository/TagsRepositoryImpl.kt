package io.aethibo.features.tags.domain.repository

import io.aethibo.core.extensions.dbQuery
import io.aethibo.features.tags.data.failure.TagException
import io.aethibo.features.tags.data.model.TagEntity
import io.aethibo.features.tags.data.repository.TagsRepository
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class TagsRepositoryImpl : TagsRepository {

    private fun validateTagName(name: String) {
        if (name.isBlank()) throw TagException.EmptyTagName
        if (!name.matches(Regex("^[a-zA-Z0-9\\-_\\s]+$"))) throw TagException.InvalidTagName(name)
    }

    override suspend fun findAll(): List<String> {
        return try {
            val tags = dbQuery {
                TagEntity.selectAll().map { it[TagEntity.name] }
            }

            if (tags.isEmpty()) throw TagException.TagsNotFound
            tags
        } catch (e: TagException) {
            throw e
        } catch (e: Exception) {
            throw TagException.DatabaseError("findAll", e)
        }
    }

    override suspend fun findByName(name: String): String? {
        return try {
            validateTagName(name)

            dbQuery {
                TagEntity.selectAll()
                    .where { TagEntity.name eq name }
                    .map { it[TagEntity.name] }
                    .firstOrNull()
            }
        } catch (e: TagException) {
            throw e
        } catch (e: Exception) {
            throw TagException.DatabaseError("findByName", e)
        }
    }

    override suspend fun create(name: String): String {
        return try {
            validateTagName(name)

            // Check if tag already exists
            val existingTag = findByName(name)
            if (existingTag != null) throw TagException.TagAlreadyExists(name)

            dbQuery {
                TagEntity.insert { row ->
                    row[TagEntity.name] = name
                }
                name
            }
        } catch (e: TagException) {
            throw e
        } catch (e: Exception) {
            throw TagException.TagCreationFailed(name)
        }
    }

    override suspend fun delete(name: String) {
        try {
            validateTagName(name)

            // Verify tag exists
            findByName(name) ?: throw TagException.TagNotFound(name)

            val deletedCount = dbQuery {
                TagEntity.deleteWhere { TagEntity.name eq name }
            }

            if (deletedCount == 0) {
                throw TagException.TagDeletionFailed
            }
        } catch (e: TagException) {
            throw e
        } catch (e: Exception) {
            throw TagException.DatabaseError("delete", e)
        }
    }

    override suspend fun exists(name: String): Boolean {
        return try {
            validateTagName(name)
            findByName(name) != null
        } catch (e: TagException) {
            false
        } catch (e: Exception) {
            throw TagException.DatabaseError("exists", e)
        }
    }
}
