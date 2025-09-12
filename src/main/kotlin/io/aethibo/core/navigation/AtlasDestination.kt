package io.aethibo.core.navigation

import io.ktor.resources.*

@Resource("/api")
class Api {
    @Resource("users")
    class Users(val parent: Api = Api()) {
        @Resource("login")
        class Login(val parent: Users = Users())
    }

    @Resource("user")
    class User(val parent: Api = Api())

    @Resource("profiles/{username}")
    class Profile(val parent: Api = Api(), val username: String) {
        @Resource("follow")
        class Follow(val parent: Profile)
    }

    @Resource("articles")
    class CreateArticle(val parent: Api = Api())

    @Resource("articles")
    class Articles(
        val parent: Api = Api(),
        val tag: String? = null,
        val author: String? = null,
        val favorited: String? = null,
        val limit: Int = 20,
        val offset: Long = 0
    ) {
        @Resource("feed")
        class Feed(
            val parent: Articles = Articles(),
            val limit: Int = 20,
            val offset: Long = 0
        )

        @Resource("{slug}")
        class Slug(val parent: Articles = Articles(), val slug: String) {
            @Resource("comments")
            class Comments(val parent: Slug) {
                @Resource("{commentId}")
                class CommentId(val parent: Comments, val commentId: Long)
            }

            @Resource("favorite")
            class Favorite(val parent: Slug)
        }
    }

    @Resource("tags")
    class Tags(val parent: Api = Api())
}
