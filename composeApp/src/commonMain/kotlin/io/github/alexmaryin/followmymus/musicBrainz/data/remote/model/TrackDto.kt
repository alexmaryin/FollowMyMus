package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String,
    val position: Int,
    val recording: RecordingDto,
)