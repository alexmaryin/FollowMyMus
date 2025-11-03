package io.github.alexmaryin.followmymus.screens.login.domain

import androidx.compose.foundation.text.input.TextFieldState

data class LoginState(
    val nickname: TextFieldState = TextFieldState(),
    val password: TextFieldState = TextFieldState(),
    val isLoading: Boolean = false,
    val isCredentialsValid: Boolean = true,
    val isQrScanOpen: Boolean = false,
)
