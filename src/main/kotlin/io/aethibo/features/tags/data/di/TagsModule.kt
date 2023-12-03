package io.aethibo.features.tags.data.di

import io.aethibo.features.tags.data.repository.TagsRepository
import io.aethibo.features.tags.domain.controller.TagsController
import io.aethibo.features.tags.domain.repository.TagsRepositoryImpl
import io.aethibo.features.tags.domain.service.TagsService
import io.aethibo.features.tags.presentation.TagsControllerImpl
import io.aethibo.features.tags.presentation.TagsServiceImpl
import org.koin.dsl.module

val tagsModule = module {
    single<TagsRepository> { TagsRepositoryImpl() }

    single<TagsService> { TagsServiceImpl(get()) }

    single<TagsController> { TagsControllerImpl(get()) }
}
