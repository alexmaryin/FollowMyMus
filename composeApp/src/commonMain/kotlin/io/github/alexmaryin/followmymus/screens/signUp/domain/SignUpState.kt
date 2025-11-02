package io.github.alexmaryin.followmymus.screens.signUp.domain

import androidx.compose.foundation.text.input.TextFieldState

data class SignUpState(
    val nickname: TextFieldState = TextFieldState(),
    val password: TextFieldState = TextFieldState(),
    val isLoading: Boolean = false,
    val isNicknameValid: Boolean = true,
    val isPasswordValid: Boolean = true
) {
    // Nickname consists only of letters, digits, underscore and dot
    val nicknameValidation get() =
        nickname.text.isNotBlank() && nickname.text.isNotEmpty() &&
        nickname.text.all { it.isLetterOrDigit() || it == '_' || it == '.' }

    // Password should be at least 6 characters long,
    // contain at least one lowercase letter, one uppercase letter, and one digit
    //may include any other symbols
    val passwordValidation
        get() = password.text.length >= 6 &&
                password.text.any { it.isLowerCase() } &&
                password.text.any { it.isUpperCase() } &&
                password.text.any { it.isDigit() }

}
