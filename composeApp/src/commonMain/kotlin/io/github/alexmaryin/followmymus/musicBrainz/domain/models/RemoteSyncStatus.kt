package io.github.alexmaryin.followmymus.musicBrainz.domain.models

import io.github.alexmaryin.followmymus.core.ErrorType

sealed interface RemoteSyncStatus {
    data object Idle : RemoteSyncStatus
    data object Process : RemoteSyncStatus
    data class Error(val errors: List<ErrorType>) : RemoteSyncStatus
}