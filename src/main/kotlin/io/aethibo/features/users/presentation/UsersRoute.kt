package io.aethibo.features.users.presentation

import io.aethibo.core.navigation.Login
import io.aethibo.core.navigation.User
import io.aethibo.core.navigation.Users
import io.aethibo.features.users.domain.controller.UsersController
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.users(userController: UsersController) {
    route(Users.route) {
        post { userController.register(call) }
        post(Login.route) { userController.login(call) }
    }
    route(User.route) {
        authenticate("jwt") {
            get { userController.getCurrent(call) }
            put { userController.update(call) }
        }
    }
}