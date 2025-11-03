package io.github.alexmaryin.followmymus.screens.login.domain

sealed class LoginAction {
    data object OnLogin : LoginAction()
    data object OnOpenSignUp : LoginAction()
    data object OnOpenQrScan : LoginAction()
    data object OnCloseQrScan : LoginAction()
    data class OnQrRecognized(val qrCode: String) : LoginAction()
    data class OnNickNameSet(val new: String) : LoginAction()
    data class OnPasswordSet(val new: String) : LoginAction()
}