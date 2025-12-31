package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchMediaResponse(
    val releases: List<MediaDto>,
    @SerialName("release-count") val count: Int,
    @SerialName("release-offset") val offset: Int = 0
)
