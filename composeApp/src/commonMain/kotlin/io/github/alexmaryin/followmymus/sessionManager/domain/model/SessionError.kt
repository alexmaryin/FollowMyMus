package io.github.alexmaryin.followmymus.sessionManager.domain.model

import io.github.alexmaryin.followmymus.core.ErrorType

sealed class SessionError : ErrorType() {
    data object AuthError : SessionError()
    data object RestError : SessionError()
    data object NetworkError : SessionError()
    data object SessionExpired : SessionError()
}