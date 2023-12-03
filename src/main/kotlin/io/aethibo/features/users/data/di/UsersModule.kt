package io.aethibo.features.users.data.di

import io.aethibo.features.users.data.repository.UsersRepositoryImpl
import io.aethibo.features.users.domain.controller.UsersController
import io.aethibo.features.users.domain.repository.UsersRepository
import io.aethibo.features.users.domain.service.UsersService
import io.aethibo.features.users.presentation.UsersControllerImpl
import io.aethibo.features.users.presentation.UsersServiceImpl
import org.koin.dsl.module

val usersModule = module {
    single<UsersRepository> { UsersRepositoryImpl() }

    single<UsersService> {
        UsersServiceImpl(
            jwtProvider = get(),
            cipher = get(),
            userRepository = get()
        )
    }

    single<UsersController> { UsersControllerImpl(get()) }
}
