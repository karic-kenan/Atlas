package io.aethibo.features.comments.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentWrapper(
    val comment: CreateCommentRequest
)

@Serializable
data class CreateCommentRequest(
    val body: String
)
