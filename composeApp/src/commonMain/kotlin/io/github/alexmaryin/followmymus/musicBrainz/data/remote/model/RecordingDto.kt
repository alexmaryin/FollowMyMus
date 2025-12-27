package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordingDto(
    val title: String,
    @SerialName("length") val length: Long?,
    val disambiguation: String?
)