package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseStatus {
    @SerialName("official")
    Official,
    @SerialName("promotion")
    Promotion,
    @SerialName("bootleg")
    Bootleg,
    @SerialName("pseudo-release")
    PseudoRelease,
    @SerialName("withdrawn")
    Withdrawn,
    @SerialName("expunged")
    Expunged,
    @SerialName("cancelled")
    Cancelled
}