package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ReleaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleasesDao {

    @Query("SELECT * FROM ReleaseEntity WHERE artistId = :artistId")
    fun getArtistReleases(artistId: String): Flow<List<ReleaseEntity>>

    /**
     * Releases are sorted by the type-key aggregator
     * (primaryType + secondaryTypes joined by " + ") ascending,
     * then by `firstReleaseDate DESC`. This matches the legacy
     * `groupByCategories` UX: all Albums first, then
     * "Album + EP" / "Album + Single" / etc., then the rest in
     * alphabetical order. The `groupedBy` extension in the UI
     * can then either keep the type-key grouping (headers
     * at the type boundaries) or apply a secondary grouping
     * (e.g. year bucket) on top of this stable order.
     */
    @Query(
        "SELECT * FROM ReleaseEntity WHERE artistId = :artistId " +
            "ORDER BY (primaryType || COALESCE(NULLIF(' + ' || REPLACE(secondaryTypes, ',', ' + '), ' + '), '')) ASC, " +
            "firstReleaseDate DESC"
    )
    fun getPagedArtistReleases(artistId: String): PagingSource<Int, ReleaseEntity>

    @Query("SELECT COUNT(*) FROM ReleaseEntity WHERE artistId = :artistId")
    fun getArtistReleasesCount(artistId: String): Flow<Int>

    @Upsert
    suspend fun insertReleases(releases: List<ReleaseEntity>)

    @Query("UPDATE ReleaseEntity SET previewCoverUrl = :preview, fullCoverUrl = :full WHERE id = :id")
    suspend fun updateReleaseCovers(id: String, preview: String?, full: String?)

    @Query("DELETE FROM ReleaseEntity WHERE artistId = :artistId")
    suspend fun clearReleases(artistId: String)

    @Query("DELETE FROM ReleaseEntity")
    suspend fun clear()
}
