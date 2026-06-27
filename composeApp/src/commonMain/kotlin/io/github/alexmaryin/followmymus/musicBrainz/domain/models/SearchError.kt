package io.github.alexmaryin.followmymus.musicBrainz.domain.models

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.paging.PagingError

sealed class SearchError : ErrorType() {
    data object InvalidResponse : SearchError()
    data class ServerError(val code: Int, val message: String) : SearchError()
    data class NetworkError(val message: String?) : SearchError()
}

fun SearchError.toPagingError(): PagingError = when (this) {
    SearchError.InvalidResponse -> PagingError.InvalidResponse
    is SearchError.NetworkError -> PagingError.Network(message)
    is SearchError.ServerError -> PagingError.Server(code, message)
}