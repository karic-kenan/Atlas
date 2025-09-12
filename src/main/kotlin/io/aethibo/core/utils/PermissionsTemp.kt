package io.aethibo.core.utils

import io.aethibo.features.users.domain.model.User

fun getUserPermissions(user: User): List<String> {
    // Just a placeholder, could at some point use custom business logic implementation
    // This could query a separate permissions/roles table
    return when {
        user.email.endsWith("@admin.com") -> listOf("read:all", "write:all", "delete:all")
        user.isActive -> listOf("read:profile", "update:profile")
        else -> emptyList()
    }
}
