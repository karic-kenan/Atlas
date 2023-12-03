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
