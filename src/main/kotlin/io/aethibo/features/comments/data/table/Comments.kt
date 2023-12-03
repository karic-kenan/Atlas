package io.aethibo.features.comments.data.table

import io.aethibo.features.comments.domain.model.Comment
import io.aethibo.features.users.domain.model.User
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow

internal object Comments : LongIdTable() {
    val body: Column<String> = varchar("body", 1000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val slug: Column<String> = varchar("slug", 100)
    val author: Column<Long> = long("author")

    fun toDomain(row: ResultRow, author: User?): Comment {
        return Comment(
            id = row[Comments.id].value,
            body = row[Comments.body],
            createdAt = row[Comments.createdAt],
            updatedAt = row[Comments.updatedAt],
            author = author,
        )
    }
}
