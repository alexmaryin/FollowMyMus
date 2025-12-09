package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus

@Dao
interface ArtistDao {

    @Query("SELECT EXISTS(SELECT 1 FROM ArtistEntity WHERE id = :artistId)")
    suspend fun isArtistExists(artistId: String): Boolean

    @Query("UPDATE ArtistEntity SET syncStatus = :newStatus, isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateSyncStatus(id: String, newStatus: SyncStatus, isFavorite: Boolean = true)

    @Upsert
    suspend fun insertArtist(artist: ArtistEntity)

    @Upsert
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM ArtistEntity WHERE id = :id")
    suspend fun deleteArtist(id: String)

    @Query("DELETE FROM ArtistEntity")
    suspend fun clearArtists()
}
