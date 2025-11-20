package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus {
    PendingRemoteAdd,
    PendingRemoteRemove,
    OK
}