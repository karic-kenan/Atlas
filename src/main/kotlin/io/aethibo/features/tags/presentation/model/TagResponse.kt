package io.aethibo.features.tags.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class TagsResponseDto(
    val tags: List<String>
)
