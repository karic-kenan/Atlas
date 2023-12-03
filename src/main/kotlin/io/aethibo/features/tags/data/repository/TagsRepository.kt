package io.aethibo.features.tags.data.repository

interface TagsRepository {
    suspend fun findAll(): List<String>
}
