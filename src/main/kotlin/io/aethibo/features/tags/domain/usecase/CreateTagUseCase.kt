package io.aethibo.features.tags.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.TagName
import io.aethibo.features.tags.data.failure.TagException
import io.aethibo.features.tags.data.failure.TagFailure
import io.aethibo.features.tags.data.failure.mapToFailure
import io.aethibo.features.tags.data.repository.TagsRepository

fun interface CreateTagUseCase : suspend (TagName) -> Either<TagFailure, String>

suspend fun createTag(
    name: TagName,
    tagsRepository: TagsRepository
): Either<TagFailure, String> = either {
    catch({
        val validName = name.takeIf { it.isNotBlank() }
            ?: raise(TagFailure.EmptyTagName)

        // Additional validation for tag name format
        if (!validName.matches(Regex("^[a-zA-Z0-9\\-_\\s]+$"))) {
            raise(TagFailure.InvalidTagName(validName))
        }

        tagsRepository.create(validName)
    }) { exception ->
        val failure = when (exception) {
            is TagException -> exception.mapToFailure()
            else -> TagFailure.DatabaseError("create tag", exception)
        }
        raise(failure)
    }
}
