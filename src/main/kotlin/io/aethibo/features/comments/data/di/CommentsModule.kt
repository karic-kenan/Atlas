package io.aethibo.features.comments.data.di

import io.aethibo.features.comments.data.repository.CommentsRepositoryImpl
import io.aethibo.features.comments.domain.controller.CommentsController
import io.aethibo.features.comments.domain.repository.CommentsRepository
import io.aethibo.features.comments.domain.service.CommentsService
import io.aethibo.features.comments.presentation.CommentsControllerImpl
import io.aethibo.features.comments.presentation.CommentsServiceImpl
import org.koin.dsl.module

val commentsModule = module {
    single<CommentsRepository> { CommentsRepositoryImpl() }

    single<CommentsService> { CommentsServiceImpl(get()) }

    single<CommentsController> { CommentsControllerImpl(get()) }
}
