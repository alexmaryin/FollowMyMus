package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchArtistResponse

interface SearchEngine {
    suspend fun searchArtists(query: String, offset: Int = 0, limit: Int = LIMIT): SearchArtistResponse

    fun getArtistFromCache(artistId: String): ArtistDto?

    suspend fun fetchArtistsById(ids: List<String>): Result<List<ArtistDto>>

    suspend fun searchReleases(artistId: String): Result<ArtistDto>

    suspend fun searchMedia(releaseId: String): Result<List<MediaDto>>

    companion object {
        const val MB_BASE_URL = "https://musicbrainz.org/ws/2"
        const val LIMIT = 50
    }
}