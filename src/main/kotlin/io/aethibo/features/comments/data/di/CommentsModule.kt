package io.aethibo.features.comments.data.di

import io.aethibo.features.comments.data.repository.CommentRepositoryImpl
import io.aethibo.features.comments.domain.repository.CommentRepository
import io.aethibo.features.comments.domain.usecase.*
import org.koin.dsl.module

val commentsModule = module {
    factory<CreateCommentUseCase> {
        CreateCommentUseCase { email, slug, comment ->
            createComment(
                email = email,
                slug = slug,
                comment = comment,
                commentsRepository = get(),
            )
        }
    }

    factory<FindCommentsUseCase> {
        FindCommentsUseCase { slug ->
            findComments(
                slug = slug,
                commentsRepository = get()
            )
        }
    }

    factory<DeleteCommentUseCase> {
        DeleteCommentUseCase { commentId, slug ->
            deleteComment(
                commentId = commentId,
                slug = slug,
                commentsRepository = get()
            )
        }
    }

    single<CommentRepository> {
        CommentRepositoryImpl(
            meterRegistry = get()
        )
    }
}
