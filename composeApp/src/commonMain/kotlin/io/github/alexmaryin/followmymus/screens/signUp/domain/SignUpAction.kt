package io.github.alexmaryin.followmymus.screens.signUp.domain

sealed class SignUpAction {
    data object OnSignUp : SignUpAction()
    data object OnOpenLogin : SignUpAction()
    data class NicknameChange(val new: String) : SignUpAction()
    data class PasswordChange(val new: String) : SignUpAction()
}