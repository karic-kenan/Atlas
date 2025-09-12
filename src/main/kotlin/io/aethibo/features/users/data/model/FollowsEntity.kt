package io.aethibo.features.users.data.model

import org.jetbrains.exposed.v1.core.Table

object FollowsEntity : Table("follows") {
    val user = reference("user", UserEntity)
    val follower = reference("follower", UserEntity)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(user, follower, name = "follows_pk")

    init {
        index(false, user)
        index(false, follower)
    }
}
