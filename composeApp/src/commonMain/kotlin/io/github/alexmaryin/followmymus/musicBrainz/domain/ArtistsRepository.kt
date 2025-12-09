package io.github.alexmaryin.followmymus.musicBrainz.domain

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ArtistsRepository {
    val searchCount: StateFlow<Int?>
    fun searchArtists(query: String): Flow<PagingData<Artist>>
    fun getFavoriteArtists(sort: Flow<SortArtists>): Flow<Map<out SortKeyType, List<FavoriteArtist>>>
    fun getFavoriteArtistsIds(): Flow<List<String>>
    suspend fun addToFavorite(artistId: String)
    suspend fun cacheArtist(artistId: String)
    suspend fun deleteFromFavorites(artistId: String)
}