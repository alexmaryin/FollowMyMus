package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.ErrorType

sealed class SearchError : ErrorType() {
    data object InvalidResponse : SearchError()
    data class ServerError(val code: Int, val message: String) : SearchError()
    data class NetworkError(val message: String?) : SearchError()
}