package io.aethibo.features.users.domain.mapper

import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.presentation.model.*
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toUserDomain(): User = User(
    id = this[UserEntity.id].value,
    email = this[UserEntity.email],
    username = this[UserEntity.username],
    password = this[UserEntity.password],
    bio = this[UserEntity.bio],
    image = this[UserEntity.image]
)

fun User.toUserResponseDto(): UserResponseDto = UserResponseDto(
    id = this.id,
    email = this.email,
    token = this.token,
    username = this.username,
    bio = this.bio,
    image = this.image
)

fun RegisterUserRequest.toDomain() = User(
    email = email,
    password = password,
    username = username
)

fun LoginUserRequest.toDomain() = User(
    email = email,
    password = password
)

fun UpdateUserRequest.toDomain() = User(
    email = email
)

fun Profile.toProfileResponseDto() = ProfileResponseDto(
    username = username,
    bio = bio,
    image = image,
    following = following
)
