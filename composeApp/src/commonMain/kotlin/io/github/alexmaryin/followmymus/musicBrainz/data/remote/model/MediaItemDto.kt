package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaItemDto(
    val id: String,
    val position: Int = 1,
    val format: String,
    val title: String,
    @SerialName("track-count") val trackCount: Int,
    @SerialName("track-offset") val trackOffset: Int = 0,
    val tracks: List<TrackDto> = emptyList()
)