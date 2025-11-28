package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseType {
    @SerialName("Album")
    ALBUM,
    @SerialName("Single")
    SINGLE,
    @SerialName("EP")
    EP,
    @SerialName("Broadcast")
    BROADCAST,
    @SerialName("Other")
    OTHER
}
