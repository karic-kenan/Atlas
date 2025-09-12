package io.aethibo.features.tags.data.model

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object TagEntity : LongIdTable("tags") {
    val name = varchar("name", 100).uniqueIndex()
}
