package io.aethibo.features.profiles.data.di

import io.aethibo.features.profiles.domain.controller.ProfilesController
import io.aethibo.features.profiles.presentation.ProfilesControllerImpl
import org.koin.dsl.module

val profilesModule = module {
    single<ProfilesController> { ProfilesControllerImpl(get()) }
}
