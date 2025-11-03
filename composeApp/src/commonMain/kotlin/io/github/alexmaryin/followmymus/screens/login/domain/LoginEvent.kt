package io.github.alexmaryin.followmymus.screens.login.domain

sealed class LoginEvent {
    data class ShowError(val message: String) : LoginEvent()
}