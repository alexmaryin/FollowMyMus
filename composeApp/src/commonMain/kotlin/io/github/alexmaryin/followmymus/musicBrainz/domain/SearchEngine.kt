package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.SearchResponse

interface SearchEngine {
    suspend fun searchArtists(query: String, offset: Int = 0, limit: Int = LIMIT): SearchResponse

    suspend fun fetchArtistsById(ids: List<String>): Result<List<ArtistDto>>

    companion object {
        const val MB_BASE_URL = "https://musicbrainz.org/ws/2"
        const val LIMIT = 50
    }
}