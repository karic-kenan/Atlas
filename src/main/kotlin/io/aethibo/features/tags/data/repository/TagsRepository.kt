package io.aethibo.features.tags.data.repository

interface TagsRepository {
    suspend fun findAll(): List<String>
    suspend fun findByName(name: String): String?
    suspend fun create(name: String): String
    suspend fun delete(name: String)
    suspend fun exists(name: String): Boolean
}
