package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query("SELECT * FROM ResourceEntity WHERE artistId = :artistId")
    fun getArtistResources(artistId: String): Flow<List<ResourceEntity>>

    @Upsert
    suspend fun insertResources(resources: List<ResourceEntity>)

    @Query("DELETE FROM ResourceEntity WHERE artistId = :artistId")
    suspend fun clearResources(artistId: String)

    @Query("DELETE FROM ResourceEntity")
    suspend fun clear()
}
