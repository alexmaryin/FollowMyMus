package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus {
    PendingRemoteAdd,
    PendingRemoteRemove,
    OK
}