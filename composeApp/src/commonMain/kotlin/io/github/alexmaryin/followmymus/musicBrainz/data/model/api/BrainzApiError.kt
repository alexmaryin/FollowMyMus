package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.ErrorType
import org.jetbrains.compose.resources.getString

sealed class BrainzApiError : ErrorType() {
    data class NetworkError(val message: String?) : BrainzApiError()
    data object Timeout : BrainzApiError()
    data object InvalidResponse : BrainzApiError()
    data object MappingError : BrainzApiError()
    data object NoCoverError : BrainzApiError()
    data class ServerError(val message: String?) : BrainzApiError()
    data class Unknown(val message: String?) : BrainzApiError()
}

suspend fun ErrorType.getUiDescription() = when (this) {
    BrainzApiError.InvalidResponse -> getString(Res.string.invalid_response_api)

    BrainzApiError.MappingError -> getString(Res.string.mapping_error)

    is BrainzApiError.NetworkError if message != null ->
        getString(Res.string.network_error_msg, message)

    is BrainzApiError.ServerError -> getString(Res.string.server_error_code, message ?: "")

    BrainzApiError.Timeout -> getString(Res.string.timeout_error_msg)

    BrainzApiError.NoCoverError -> getString(Res.string.no_cover_error_msg)

    is BrainzApiError.Unknown -> getString(Res.string.network_error_msg, message ?: "")

    else -> null
}