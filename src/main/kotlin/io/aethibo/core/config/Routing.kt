package io.aethibo.core.config

import io.aethibo.features.articles.domain.usecase.*
import io.aethibo.features.articles.presentation.article
import io.aethibo.features.comments.domain.usecase.CreateCommentUseCase
import io.aethibo.features.comments.domain.usecase.DeleteCommentUseCase
import io.aethibo.features.comments.domain.usecase.FindCommentsUseCase
import io.aethibo.features.comments.presentation.comment
import io.aethibo.features.tags.domain.usecase.GetAllTagsUseCase
import io.aethibo.features.tags.presentation.tag
import io.aethibo.features.users.domain.usecase.*
import io.aethibo.features.users.presentation.profile
import io.aethibo.features.users.presentation.user
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val findFeedUseCase: FindFeedUseCase by inject()
    val createArticleUseCase: CreateArticleUseCase by inject()
    val updateArticleUseCase: UpdateArticleUseCase by inject()
    val deleteArticleUseCase: DeleteArticleUseCase by inject()
    val favouriteArticleUseCase: FavoriteArticleUseCase by inject()
    val unFavouriteArticleUseCase: UnfavoriteArticleUseCase by inject()
    val findArticlesUseCase: FindArticlesUseCase by inject()
    val findArticleBySlugUseCase: GetArticleBySlugUseCase by inject()

    val createCommentUseCase: CreateCommentUseCase by inject()
    val findCommentsUseCase: FindCommentsUseCase by inject()
    val deleteCommentUseCase: DeleteCommentUseCase by inject()

    val authenticateUserUseCase: AuthenticateUserUseCase by inject()
    val createUserUseCase: CreateUserUseCase by inject()
    val updateUserUseCase: UpdateUserUseCase by inject()
    val getProfileByUsernameUseCase: GetProfileByUsernameUseCase by inject()
    val followProfileUseCase: FollowProfileUseCase by inject()
    val unfollowProfileUseCase: UnfollowProfileUseCase by inject()

    val getAllTagsUseCase: GetAllTagsUseCase by inject()

    install(Resources)

    routing {
        user(
            authenticateUserUseCase = authenticateUserUseCase,
            createUserUseCase = createUserUseCase,
            updateUserUseCase = updateUserUseCase
        )
        profile(
            getProfileByUsernameUseCase = getProfileByUsernameUseCase,
            followProfileUseCase = followProfileUseCase,
            unfollowProfileUseCase = unfollowProfileUseCase
        )
        article(
            findFeedUseCase = findFeedUseCase,
            findArticlesUseCase = findArticlesUseCase,
            getArticleBySlugUseCase = findArticleBySlugUseCase,
            createArticleUseCase = createArticleUseCase,
            updateArticleUseCase = updateArticleUseCase,
            deleteArticleUseCase = deleteArticleUseCase,
            favouriteArticleUseCase = favouriteArticleUseCase,
            unfavoriteArticleUseCase = unFavouriteArticleUseCase
        )
        comment(
            findCommentsUseCase = findCommentsUseCase,
            createCommentUseCase = createCommentUseCase,
            deleteCommentUseCase = deleteCommentUseCase
        )
        tag(getAllTagsUseCase = getAllTagsUseCase)
    }
}
