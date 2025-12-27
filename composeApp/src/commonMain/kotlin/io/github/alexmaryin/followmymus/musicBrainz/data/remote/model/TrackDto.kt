package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String,
    val position: Int,
    val title: String,
    @SerialName("length") val lengthMs: Int?,
)