package io.github.alexmaryin.followmymus.screens.login.domain

import kotlinx.serialization.Serializable

@Serializable
data class LoginState(
    val nickname: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isCredentialsValid: Boolean = true,
    val isQrScanOpen: Boolean = false,
)
