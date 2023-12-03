package io.aethibo.features.articles.data.di

import io.aethibo.features.articles.data.repository.ArticlesRepositoryImpl
import io.aethibo.features.articles.domain.controller.ArticlesController
import io.aethibo.features.articles.domain.repository.ArticlesRepository
import io.aethibo.features.articles.domain.service.ArticlesService
import io.aethibo.features.articles.presentation.ArticlesControllerImpl
import io.aethibo.features.articles.presentation.ArticlesServiceImpl
import org.koin.dsl.module

val articlesModule = module {
    single<ArticlesRepository> { ArticlesRepositoryImpl() }

    single<ArticlesService> { ArticlesServiceImpl(get(), get(), get()) }

    single<ArticlesController> { ArticlesControllerImpl(get()) }
}