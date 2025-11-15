package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val created: String,
    val count: Int,
    val offset: Int,
    val artists: List<ArtistDto>
)
