package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.core.ErrorType

sealed class BrainzApiError : ErrorType() {
    data class NetworkError(val message: String?) : BrainzApiError()
    data object Timeout : BrainzApiError()
    data object InvalidResponse : BrainzApiError()
    data object MappingError : BrainzApiError()
    data object NoCoverError : BrainzApiError()
    data class ServerError(val message: String?) : BrainzApiError()
    data class Unknown(val message: String?) : BrainzApiError()
}