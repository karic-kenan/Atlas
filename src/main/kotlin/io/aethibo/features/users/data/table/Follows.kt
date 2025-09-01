package io.aethibo.features.users.data.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

internal object Follows : Table() {
    val user: Column<Long> = long("user")
    val follower: Column<Long> = long("user_follower")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            user,
            follower,
            name = "followsKey"
        )
}
