package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.ArtistsResult
import kotlinx.coroutines.flow.Flow

interface ArtistsRepository {
    fun searchArtists(query: String): Flow<ArtistsResult>
    fun getFavoriteArtists(): Flow<Artist>
    fun addToFavorite(artist: Artist)
    fun deleteFromFavorites(artistId: Int)
}