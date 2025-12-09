package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.CoverArtResponse
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.SearchResponse

interface SearchEngine {
    suspend fun searchArtists(query: String, offset: Int = 0, limit: Int = LIMIT): SearchResponse

    fun getArtistFromCache(artistId: String): ArtistDto?

    suspend fun fetchArtistsById(ids: List<String>): Result<List<ArtistDto>>

    suspend fun searchReleases(artistId: String): Result<ArtistDto>

    suspend fun searchCovers(releaseId: String): Result<CoverArtResponse>

    companion object {
        const val MB_BASE_URL = "https://musicbrainz.org/ws/2"
        const val COVER_ART_BASE_URL = "https://coverartarchive.org"
        const val LIMIT = 50
    }
}