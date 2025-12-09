package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.ErrorType

sealed interface RemoteSyncStatus {
    data object Idle : RemoteSyncStatus
    data object Process : RemoteSyncStatus
    data class Error(val errors: List<ErrorType>) : RemoteSyncStatus
}