package io.aethibo.features.comments.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

internal object CommentEntity : LongIdTable() {
    val body: Column<String> = varchar("body", 1000)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    val slug: Column<String> = varchar("slug", 100)
    val author: Column<Long> = long("author")
}
