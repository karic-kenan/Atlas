package io.aethibo.features.comments.data.table

import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

internal object Comments : LongIdTable() {
    val body: Column<String> = varchar("body", 1000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val slug: Column<String> = varchar("slug", 100)
    val author: Column<Long> = long("author")

    fun toDomain(row: ResultRow, author: User?): Comment {
        return Comment(
            id = row[id].value,
            body = row[body],
            createdAt = row[createdAt],
            updatedAt = row[updatedAt],
            author = author,
        )
    }
}
