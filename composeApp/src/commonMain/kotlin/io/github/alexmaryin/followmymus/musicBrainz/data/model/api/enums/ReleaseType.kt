package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseType {
    @SerialName("Album")
    ALBUM,
    @SerialName("EP")
    EP,
    @SerialName("Single")
    SINGLE,
    @SerialName("Broadcast")
    BROADCAST,
    @SerialName("Other")
    OTHER
}
