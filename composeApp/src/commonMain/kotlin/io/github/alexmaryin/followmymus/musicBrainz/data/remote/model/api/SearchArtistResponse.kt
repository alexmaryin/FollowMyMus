package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import kotlinx.serialization.Serializable

@Serializable
data class SearchArtistResponse(
    val created: String,
    val count: Int,
    val offset: Int,
    val artists: List<ArtistDto>
)