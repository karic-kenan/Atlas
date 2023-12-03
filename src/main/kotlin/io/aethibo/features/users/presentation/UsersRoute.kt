package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Login
import io.aethibo.core.navigation.User
import io.aethibo.core.navigation.Users
import io.aethibo.features.users.domain.controller.UsersController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.users(userController: UsersController) {
    route(Users.route) {
        post { userController.register(this.context) }
        post(Login.route) { userController.login(this.context) }
    }
    route(User.route) {
        authenticate("jwt") {
            get { userController.getCurrent(this.context) }
            put { userController.update(this.context) }
        }
    }
}