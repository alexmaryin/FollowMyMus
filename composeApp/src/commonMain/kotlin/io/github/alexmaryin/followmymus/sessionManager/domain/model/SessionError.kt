package io.github.alexmaryin.followmymus.sessionManager.domain.model

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.jan.supabase.auth.exception.AuthErrorCode

sealed class SessionError : ErrorType() {
    data class AuthError(val code: AuthErrorCode) : SessionError()
    data class RestError(val statusCode: Int) : SessionError()
    data object NetworkError : SessionError()
    data object SessionExpired : SessionError()
}