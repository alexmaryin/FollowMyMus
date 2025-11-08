package io.github.alexmaryin.followmymus.rootNavigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Config {
    @Serializable
    data class Login(val qrCode: String? = null) : Config()

    @Serializable
    data object SignUp : Config()

    @Serializable
    data class MainScreen(val nickname: String) : Config()

    @Serializable
    data object Splash : Config()
}