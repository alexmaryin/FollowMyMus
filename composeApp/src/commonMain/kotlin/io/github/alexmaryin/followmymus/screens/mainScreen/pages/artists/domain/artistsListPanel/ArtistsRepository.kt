package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ArtistsRepository {
    val searchCount: StateFlow<Int?>
    val syncStatus: StateFlow<RemoteSyncStatus>
    val hasPendingActions: StateFlow<Boolean>

    fun searchArtists(query: String): Flow<PagingData<Artist>>
    fun getFavoriteArtists(): Flow<List<FavoriteArtist>>
    fun getFavoriteArtistsIds(): Flow<List<String>>
    suspend fun addToFavorite(artist: Artist)
    suspend fun deleteFromFavorites(artistId: String)
    suspend fun checkPendingActions()
    suspend fun syncRemote()
    suspend fun clearLocalData()

    enum class RemoteSyncStatus {
        IDLE, PROCESS, ERROR
    }
}