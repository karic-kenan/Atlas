package io.aethibo.features.tags.domain.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import io.aethibo.core.utils.TagName
import io.aethibo.features.tags.data.failure.TagException
import io.aethibo.features.tags.data.failure.TagFailure
import io.aethibo.features.tags.data.failure.mapToFailure
import io.aethibo.features.tags.domain.repository.TagsRepository

fun interface DeleteTagUseCase : suspend (TagName) -> Either<TagFailure, Unit>

suspend fun deleteTag(
    name: TagName,
    tagsRepository: TagsRepository
): Either<TagFailure, Unit> = either {
    catch({
        val validName = name.takeIf { it.isNotBlank() }
            ?: raise(TagFailure.EmptyTagName)

        tagsRepository.delete(validName)
    }) { exception ->
        val failure = when (exception) {
            is TagException -> exception.mapToFailure()
            else -> TagFailure.DatabaseError("delete tag", exception)
        }
        raise(failure)
    }
}
