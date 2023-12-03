package io.aethibo.features.tags.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

internal object Tags : LongIdTable() {
    val name: Column<String> = varchar("name", 100).uniqueIndex()
}
