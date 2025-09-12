package io.aethibo.core.di

import com.github.slugify.Slugify
import io.aethibo.core.security.JwtProvider
import io.aethibo.core.security.RedisTokenBlacklistService
import io.aethibo.core.security.SecureArgon2Cipher
import io.aethibo.core.security.TokenBlacklistService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.dsl.module

val appModule = module {
    single<MeterRegistry> { PrometheusMeterRegistry(PrometheusConfig.DEFAULT) }
    single { JwtProvider(get()) }
    single { SecureArgon2Cipher }
    single { Slugify.builder().build() }
    single<TokenBlacklistService> { RedisTokenBlacklistService() }
}
