package io.aethibo.features.tags.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateTagRequest(
    val name: String
)
