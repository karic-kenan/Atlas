package io.aethibo.features.tags.domain.repository

import io.aethibo.core.config.DatabaseFactory.dbQuery
import io.aethibo.features.tags.data.repository.TagsRepository
import io.aethibo.features.tags.data.table.Tags
import org.jetbrains.exposed.sql.selectAll

class TagsRepositoryImpl : TagsRepository {
    override suspend fun findAll(): List<String> = dbQuery {
        Tags.selectAll().map { it[Tags.name] }
    }
}
