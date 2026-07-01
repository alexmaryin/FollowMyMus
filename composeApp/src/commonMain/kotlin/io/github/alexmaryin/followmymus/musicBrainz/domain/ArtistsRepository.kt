package io.github.alexmaryin.followmymus.musicBrainz.domain

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.coroutines.flow.Flow

interface ArtistsRepository {
    val totalArtistCount: Flow<Int?>
    val totalFavoritesCount: Flow<Int?>
    fun searchArtists(query: String): Flow<PagingData<Artist>>
    fun getFavoriteArtists(sort: Flow<SortArtists>): Flow<PagingData<FavoriteArtist>>
    fun getFavoriteArtistsIds(): Flow<List<String>>
    suspend fun addToFavorite(artistId: String)
    suspend fun addToFavoritesBulk(artists: List<ArtistDto>)
    suspend fun cacheArtist(artistId: String)
    suspend fun deleteFromFavorites(artistId: String)
}
