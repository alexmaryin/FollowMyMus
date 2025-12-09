package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto

interface LocalDbRepository {
    suspend fun insertArtist(artist: ArtistDto, transform: ArtistDto.() -> ArtistEntity)
    suspend fun bulkInsertArtists(artists: List<ArtistDto>, transform: ArtistDto.() -> ArtistEntity)
    suspend fun clearLocalData()
}