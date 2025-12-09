package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SyncDao {
    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true AND syncStatus = 'PendingRemoteAdd'")
    suspend fun getIdsToPushAsList(): List<String>

    @Query("SELECT id FROM ArtistEntity")
    suspend fun getIdsAsList(): List<String>

    @Query("SELECT id FROM ArtistEntity WHERE syncStatus = 'PendingRemoteRemove'")
    suspend fun getArtistsIdsPendingRemove(): List<String>

    @Query("UPDATE ArtistEntity SET syncStatus = 'OK' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: Set<String>)

    @Query("SELECT EXISTS(SELECT * FROM ArtistEntity WHERE syncStatus <> 'OK')")
    suspend fun hasPendingActions(): Boolean

    @Query("DELETE FROM ArtistEntity WHERE id IN (:ids)")
    suspend fun bulkDeleteArtistsById(ids: List<String>)
}