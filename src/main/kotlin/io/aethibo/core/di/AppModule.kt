package io.aethibo.core.di

import com.github.slugify.Slugify
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.RedisTokenBlacklistService
import io.aethibo.core.security.SecureArgon2Cipher
import io.aethibo.core.security.TokenBlacklistService
import org.koin.dsl.module

val appModule = module {
    single { JwtProvider(get()) }
    single { SecureArgon2Cipher }
    single { Slugify.builder().build() }
    single<TokenBlacklistService> { RedisTokenBlacklistService() }
}
