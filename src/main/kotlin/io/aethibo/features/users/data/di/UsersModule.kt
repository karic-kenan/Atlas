package io.aethibo.features.users.data.di

import io.aethibo.features.users.data.repository.UsersRepositoryImpl
import io.aethibo.features.users.domain.repository.UsersRepository
import io.aethibo.features.users.domain.usecase.*
import org.koin.dsl.module

val usersModule = module {

    factory<AuthenticateUserUseCase> {
        AuthenticateUserUseCase { user ->
            authenticateUser(
                userRepository = get(),
                jwtProvider = get(),
                user = user
            )
        }
    }

    factory<CreateUserUseCase> {
        CreateUserUseCase { user ->
            createUser(
                userRepository = get(),
                jwtProvider = get(),
                user = user
            )
        }
    }

    factory<UpdateUserUseCase> {
        UpdateUserUseCase { email, user ->
            updateUser(
                userRepository = get(),
                email = email,
                user = user
            )
        }
    }

    factory<GetUserByIdUseCase> {
        GetUserByIdUseCase { id ->
            getUserById(
                userRepository = get(),
                jwtProvider = get(),
                id = id
            )
        }
    }

    factory<GetUserByEmailUseCase> {
        GetUserByEmailUseCase { email ->
            getUserByEmail(
                userRepository = get(),
                jwtProvider = get(),
                email = email
            )
        }
    }

    factory<GetProfileByUsernameUseCase> {
        GetProfileByUsernameUseCase { email, username ->
            getProfileByUsername(
                userRepository = get(),
                email = email,
                username = username
            )
        }
    }

    factory<FollowProfileUseCase> {
        FollowProfileUseCase { email, usernameToFollow ->
            followProfile(
                userRepository = get(),
                email = email,
                usernameToFollow = usernameToFollow
            )
        }
    }

    factory<UnfollowProfileUseCase> {
        UnfollowProfileUseCase { email, usernameToUnfollow ->
            unfollowProfile(
                userRepository = get(),
                email = email,
                usernameToUnfollow = usernameToUnfollow
            )
        }
    }

    factory<RefreshTokenUseCase> {
        RefreshTokenUseCase { refreshToken ->
            refreshToken(
                userRepository = get(),
                jwtProvider = get(),
                tokenBlacklistService = get(),
                refreshToken = refreshToken
            )
        }
    }

    single<UsersRepository> { UsersRepositoryImpl() }
}
