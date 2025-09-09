package io.aethibo.features.users.domain.mapper

import io.aethibo.features.users.data.model.UserEntity
import io.aethibo.features.users.domain.model.Profile
import io.aethibo.features.users.domain.model.User
import io.aethibo.features.users.presentation.model.*
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.format.DateTimeFormatter

fun ResultRow.toUserDomain(): User = User(
    id = this[UserEntity.id].value,
    email = this[UserEntity.email],
    username = this[UserEntity.username],
    password = this[UserEntity.password],
    bio = this[UserEntity.bio],
    image = this[UserEntity.image],
    token = this[UserEntity.token],
    refreshToken = this[UserEntity.refreshToken],
    isActive = this[UserEntity.isActive],
    emailVerified = this[UserEntity.emailVerified],
    lastLoginAt = this[UserEntity.lastLoginAt],
    failedLoginAttempts = this[UserEntity.failedLoginAttempts],
    lockedUntil = this[UserEntity.lockedUntil],
    createdAt = this[UserEntity.createdAt],
    updatedAt = this[UserEntity.updatedAt]
)

fun User.toUserResponseDto(): UserResponseDto {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    return UserResponseDto(
        id = this.id,
        email = this.email,
        username = this.username,
        password = this.password,
        bio = this.bio,
        image = this.image,
        token = this.token,
        refreshToken = this.refreshToken,
        createdAt = this.createdAt?.format(formatter),
        updatedAt = this.updatedAt?.format(formatter),
        isActive = this.isActive,
        emailVerified = this.emailVerified,
        lastLoginAt = this.lastLoginAt?.format(formatter),
        failedLoginAttempts = this.failedLoginAttempts,
        lockedUntil = this.lockedUntil?.format(formatter)
    )
}

fun User.toCreatedResponseDto(): UserCreatedResponseDto {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    return UserCreatedResponseDto(
        id = this.id,
        email = this.email,
        username = this.username,
        bio = this.bio,
        image = this.image,
        token = this.token ?: "",
        refreshToken = this.refreshToken ?: "",
        createdAt = this.createdAt?.format(formatter),
        updatedAt = this.updatedAt?.format(formatter)
    )
}

fun User.toUpdatedResponseDto(): UserUpdatedResponseDto {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    return UserUpdatedResponseDto(
        id = this.id,
        email = this.email,
        username = this.username,
        bio = this.bio,
        image = this.image,
        createdAt = this.createdAt?.format(formatter),
        updatedAt = this.updatedAt?.format(formatter)
    )
}

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
    email = email!!,
    username = username,
    bio = bio,
    image = image
)

fun Profile.toProfileResponseDto() = ProfileResponseDto(
    username = username,
    bio = bio,
    image = image,
    following = following
)
