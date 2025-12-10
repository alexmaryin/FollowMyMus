package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.musicBrainz.domain.models.RemoteSyncStatus
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val syncStatus: StateFlow<RemoteSyncStatus>
    val hasPendingActions: StateFlow<Boolean>
    suspend fun checkPendingActions()
    suspend fun syncRemote()
}