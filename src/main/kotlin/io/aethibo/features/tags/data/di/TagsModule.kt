package io.aethibo.features.tags.data.di

import io.aethibo.features.tags.data.repository.TagsRepository
import io.aethibo.features.tags.domain.repository.TagsRepositoryImpl
import io.aethibo.features.tags.domain.usecase.*
import org.koin.dsl.module

val tagsModule = module {
    factory<GetAllTagsUseCase> {
        GetAllTagsUseCase {
            getAllTags(
                tagsRepository = get()
            )
        }
    }

    factory<GetTagByNameUseCase> {
        GetTagByNameUseCase { tag ->
            getTagByName(
                name = tag,
                tagsRepository = get()
            )
        }
    }

    factory<CreateTagUseCase> {
        CreateTagUseCase { tag ->
            createTag(
                name = tag,
                tagsRepository = get()
            )
        }
    }

    factory<CheckTagExistsUseCase> {
        CheckTagExistsUseCase { tag ->
            checkTagExists(
                name = tag,
                tagsRepository = get()
            )
        }
    }

    factory<DeleteTagUseCase> {
        DeleteTagUseCase { tag ->
            deleteTag(
                name = tag,
                tagsRepository = get()
            )
        }
    }

    single<TagsRepository> { TagsRepositoryImpl() }
}
