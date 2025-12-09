package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ReleaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleasesDao {

    @Query("SELECT * FROM ReleaseEntity WHERE artistId = :artistId")
    fun getArtistReleases(artistId: String): Flow<List<ReleaseEntity>>

    @Upsert
    suspend fun insertReleases(releases: List<ReleaseEntity>)

    @Query("UPDATE ReleaseEntity SET previewCoverUrl = :preview, fullCoverUrl = :full WHERE id = :id")
    suspend fun updateReleaseCovers(id: String, preview: String?, full: String?)

    @Query("DELETE FROM ReleaseEntity WHERE artistId = :artistId")
    suspend fun clearReleases(artistId: String)
}
