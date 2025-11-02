package io.github.alexmaryin.followmymus.screens.signUp.domain

sealed class SignUpEvent {
    data class ShowError(val message: String) : SignUpEvent()
}