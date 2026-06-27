package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.paging.PagingError
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException

/**
 * Maps a [Throwable] thrown by a Ktor-backed paging source into a
 * [PagingError]. Used as the `errorMapper` for [io.github.alexmaryin.followmymus.core.ui.HandlePagingItems]
 * in screens whose repository is musicBrainz-aware.
 *
 * The set of recognised Ktor exceptions is intentionally narrow —
 * only the types that are present in `ktor-client-core` 3.4.x and
 * are safe to depend on from commonMain. `Unknown` is the catch-all
 * for everything else (including platform-specific Ktor engine
 * errors that don't share a common supertype).
 */
fun toPagingError(throwable: Throwable): PagingError = when (throwable) {
    is SocketTimeoutException -> PagingError.Network(throwable.message)
    is ResponseException -> PagingError.Server(
        code = throwable.response.status.value,
        message = throwable.response.status.description
    )
    else -> PagingError.Network(throwable.message)
}
