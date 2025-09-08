package io.aethibo.features.tags.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.features.tags.data.failure.TagException
import io.aethibo.features.tags.data.failure.TagFailure
import io.aethibo.features.tags.data.failure.mapToFailure
import io.aethibo.features.tags.data.repository.TagsRepository

fun interface GetAllTagsUseCase : suspend () -> Either<TagFailure, List<String>>

suspend fun getAllTags(
    tagsRepository: TagsRepository
): Either<TagFailure, List<String>> = either {
    catch({
        tagsRepository.findAll()
    }) { exception ->
        val failure = when (exception) {
            is TagException -> exception.mapToFailure()
            else -> TagFailure.DatabaseError("get all tags", exception)
        }
        raise(failure)
    }
}
