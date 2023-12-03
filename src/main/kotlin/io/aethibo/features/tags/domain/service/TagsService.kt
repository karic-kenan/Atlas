package io.aethibo.features.tags.domain.service

import io.aethibo.features.tags.domain.model.TagDTO

interface TagsService {
    suspend fun findAll(): TagDTO
}
