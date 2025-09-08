package io.aethibo.features.users.data.model

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

internal object FollowsEntity : Table() {
    val user: Column<Long> = long("user")
    val follower: Column<Long> = long("user_follower")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(
            user,
            follower,
            name = "followsKey"
        )
}
