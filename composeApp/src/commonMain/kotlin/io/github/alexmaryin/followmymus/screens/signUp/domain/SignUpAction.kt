package io.github.alexmaryin.followmymus.screens.signUp.domain

sealed class SignUpAction {
    data object TogglePasswordVisibility : SignUpAction()
    data object OnSignUp : SignUpAction()
    data object OnOpenLogin : SignUpAction()
}