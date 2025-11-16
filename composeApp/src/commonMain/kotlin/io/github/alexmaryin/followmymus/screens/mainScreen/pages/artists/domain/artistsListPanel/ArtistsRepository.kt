package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ArtistsRepository {
    val searchCount: StateFlow<Int?>
    fun searchArtists(query: String): Flow<PagingData<Artist>>
    fun getFavoriteArtists(): Flow<Artist>
    suspend fun addToFavorite(artist: Artist)
    suspend fun deleteFromFavorites(artistId: String)
}