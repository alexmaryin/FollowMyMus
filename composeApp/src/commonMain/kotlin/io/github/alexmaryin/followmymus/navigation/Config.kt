package io.github.alexmaryin.followmymus.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Config {
    @Serializable
    data class Login(val qrCode: String? = null) : Config()

    @Serializable
    data object SignUp : Config()

    @Serializable
    data object Settings : Config()

    @Serializable
    data object MainScreen : Config()
}