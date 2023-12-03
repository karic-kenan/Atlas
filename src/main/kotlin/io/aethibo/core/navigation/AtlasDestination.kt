package io.aethibo.core.navigation

interface AtlasDestination {
    val route: String
}

object Users : AtlasDestination {
    override val route: String = "users"
}

object User : AtlasDestination {
    override val route: String = "user"
}

object Login : AtlasDestination {
    override val route: String = "login"
}

object Profile : AtlasDestination {
    override val route: String = "profiles/{username}"
}

object Follow : AtlasDestination {
    override val route: String = "follow"
}

object Articles : AtlasDestination {
    override val route: String = "articles"
}

object Feed : AtlasDestination {
    override val route: String = "feed"
}

object Slug : AtlasDestination {
    override val route: String = "slug"
}

object Comments : AtlasDestination {
    override val route: String = "comments"
}

object CommentId : AtlasDestination {
    override val route: String = "commentId"
}

object Favorite : AtlasDestination {
    override val route: String = "favorite"
}

object Tags : AtlasDestination {
    override val route: String = "tags"
}
