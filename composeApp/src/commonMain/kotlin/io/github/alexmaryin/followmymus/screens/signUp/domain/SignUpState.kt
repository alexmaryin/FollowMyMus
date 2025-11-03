package io.github.alexmaryin.followmymus.screens.signUp.domain

import kotlinx.serialization.Serializable

@Serializable
data class SignUpState(
    val nickname: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isNicknameValid: Boolean = true,
    val isPasswordValid: Boolean = true
) {
    // Nickname consists only of letters, digits, underscore and dot
    val nicknameValidation get() =
        nickname.isNotBlank() && nickname.isNotEmpty() &&
        nickname.all { it.isLetterOrDigit() || it == '_' || it == '.' }

    // Password should be at least 6 characters long,
    // contain at least one lowercase letter, one uppercase letter, and one digit
    //may include any other symbols
    val passwordValidation
        get() = password.length >= 6 &&
                password.any { it.isLowerCase() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() }

}
