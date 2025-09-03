package io.aethibo.core.di

import com.github.slugify.Slugify
import io.aethibo.core.security.Cipher
import io.aethibo.core.security.JwtProvider
import org.koin.dsl.module

val appModule = module {
    single { JwtProvider(get()) }
    single { Cipher }
    single { Slugify.builder().build() }
}
