package io.aethibo.features.tags.data.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

internal object Tags : LongIdTable() {
    val name: Column<String> = varchar("name", 100).uniqueIndex()
}
