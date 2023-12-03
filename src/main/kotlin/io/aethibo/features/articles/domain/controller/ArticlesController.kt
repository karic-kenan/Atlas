package io.aethibo.features.articles.domain.controller

import io.ktor.server.application.*

interface ArticlesController {
    suspend fun findBy(call: ApplicationCall)
    suspend fun feed(call: ApplicationCall)
    suspend fun get(call: ApplicationCall)
    suspend fun create(call: ApplicationCall)
    suspend fun update(call: ApplicationCall)
    suspend fun delete(call: ApplicationCall)
    suspend fun favorite(call: ApplicationCall)
    suspend fun unfavorite(call: ApplicationCall)
}
