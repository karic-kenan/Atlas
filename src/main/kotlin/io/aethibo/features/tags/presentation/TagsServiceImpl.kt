package io.aethibo.features.tags.presentation

import io.aethibo.features.tags.data.repository.TagsRepository
import io.aethibo.features.tags.domain.model.TagDTO
import io.aethibo.features.tags.domain.service.TagsService

class TagsServiceImpl(private val tagRepository: TagsRepository) : TagsService {
    override suspend fun findAll(): TagDTO =
        TagDTO(tagRepository.findAll())
}
