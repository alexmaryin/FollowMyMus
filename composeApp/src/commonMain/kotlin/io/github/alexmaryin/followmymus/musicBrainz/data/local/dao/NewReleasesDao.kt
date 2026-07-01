package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface NewReleasesDao {

    /**
     * Ordered by `artistName ASC, firstReleaseDate DESC` so that releases from
     * the same artist are adjacent. The UI groups by `artistName` via
     * `PagingData.groupedBy` (which uses `insertSeparators` and assumes
     * adjacent same-key items); a date-only sort would scatter same-artist
     * rows and produce duplicate headers with identical LazyList keys.
     */
    @Query("SELECT * FROM new_releases WHERE state != 'DISMISSED' ORDER BY artistName ASC, firstReleaseDate DESC")
    fun getNewReleases(): PagingSource<Int, NewReleaseEntity>

    @Query("SELECT * FROM new_releases ORDER BY firstReleaseDate DESC")
    fun observeNewReleases(): Flow<List<NewReleaseEntity>>

    @Query("SELECT COUNT(*) FROM new_releases WHERE state = 'UNSEEN'")
    fun getUnseenCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewReleases(releases: List<NewReleaseEntity>)

    @Query(
        "UPDATE new_releases SET artistId = :artistId, artistName = :artistName, title = :title, " +
            "disambiguation = :disambiguation, firstReleaseDate = :firstReleaseDate, " +
            "primaryType = :primaryType, secondaryTypes = :secondaryTypes, " +
            "coverFrontUrl = :coverFrontUrl, discoveredAt = :discoveredAt " +
            "WHERE id = :id",
    )
    suspend fun updateMutableFields(
        id: String,
        artistId: String,
        artistName: String,
        title: String,
        disambiguation: String?,
        firstReleaseDate: String?,
        primaryType: ReleaseType,
        secondaryTypes: String?,
        coverFrontUrl: String?,
        discoveredAt: Instant,
    )

    /**
     * State-preserving upsert. The `INSERT OR IGNORE` writes new rows with the
     * default state (`UNSEEN`); the follow-up `UPDATE` rewrites the mutable
     * fields on every row, leaving the `state` column untouched. Existing
     * `SEEN` / `DISMISSED` rows therefore keep their state across re-syncs.
     */
    @Transaction
    suspend fun upsertNewReleases(releases: List<NewReleaseEntity>) {
        insertNewReleases(releases)
        releases.forEach { r ->
            updateMutableFields(
                id = r.id,
                artistId = r.artistId,
                artistName = r.artistName,
                title = r.title,
                disambiguation = r.disambiguation,
                firstReleaseDate = r.firstReleaseDate,
                primaryType = r.primaryType,
                secondaryTypes = r.secondaryTypes,
                coverFrontUrl = r.coverFrontUrl,
                discoveredAt = r.discoveredAt,
            )
        }
    }

    @Query("UPDATE new_releases SET coverFrontUrl = :coverFrontUrl WHERE id = :id")
    suspend fun updateCoverFrontUrl(id: String, coverFrontUrl: String?)

    @Query("UPDATE new_releases SET state = 'SEEN' WHERE id = :releaseId")
    suspend fun markSeen(releaseId: String)

    @Query("UPDATE new_releases SET state = 'DISMISSED' WHERE id = :releaseId")
    suspend fun markDismissed(releaseId: String)

    @Query("UPDATE new_releases SET state = 'UNSEEN' WHERE id = :releaseId")
    suspend fun markUnseen(releaseId: String)

    @Query("UPDATE new_releases SET state = 'UNSEEN' WHERE state = 'DISMISSED'")
    suspend fun restoreAllDismissed()

    @Query("DELETE FROM new_releases")
    suspend fun clear()
}
