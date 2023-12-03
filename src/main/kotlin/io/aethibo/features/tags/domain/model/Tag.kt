package io.aethibo.features.tags.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TagDTO(val tags: List<String>)
