package io.aethibo.features.articles.data.di

import io.aethibo.features.articles.data.repository.ArticleRepositoryImpl
import io.aethibo.features.articles.domain.repository.ArticleRepository
import io.aethibo.features.articles.domain.usecase.*
import org.koin.dsl.module

val articlesModule = module {
    factory<FindArticlesUseCase> {
        FindArticlesUseCase { limit, offset, tag, author, favourite ->
            findArticles(
                articleRepository = get(),
                limit = limit,
                offset = offset,
                tag = tag,
                author = author,
                favourite = favourite
            )
        }
    }

    factory<GetArticleBySlugUseCase> {
        GetArticleBySlugUseCase { slug ->
            getArticleBySlug(
                articleRepository = get(),
                slug = slug
            )
        }
    }

    factory<CreateArticleUseCase> {
        CreateArticleUseCase { email, article ->
            createArticle(
                userRepository = get(),
                articleRepository = get(),
                slugifyBuilder = get(),
                email = email,
                article = article
            )
        }
    }

    factory<UpdateArticleUseCase> {
        UpdateArticleUseCase { slug, article ->
            updateArticle(
                articleRepository = get(),
                slug = slug,
                article = article
            )
        }
    }

    factory<FindFeedUseCase> {
        FindFeedUseCase { email, limit, offset ->
            findFeed(
                articleRepository = get(),
                email = email,
                limit = limit,
                offset = offset
            )
        }
    }

    factory<FavoriteArticleUseCase> {
        FavoriteArticleUseCase { email, slug ->
            favoriteArticle(
                userRepository = get(),
                articleRepository = get(),
                email = email,
                slug = slug
            )
        }
    }

    factory<UnfavoriteArticleUseCase> {
        UnfavoriteArticleUseCase { email, slug ->
            unfavoriteArticle(
                userRepository = get(),
                articleRepository = get(),
                email = email,
                slug = slug
            )
        }
    }

    factory<DeleteArticleUseCase> {
        DeleteArticleUseCase { slug ->
            deleteArticle(
                articleRepository = get(),
                slug = slug
            )
        }
    }

    single<ArticleRepository> {
        ArticleRepositoryImpl(
            meterRegistry = get()
        )
    }
}
