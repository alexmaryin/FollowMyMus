package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Transaction
    @Query("SELECT * FROM MediaEntity WHERE releaseId = :releaseId")
    fun getReleaseMedia(releaseId: String): Flow<List<MediaWithData>>

    @Upsert
    suspend fun insertRawMedia(media: List<MediaEntity>)

    @Upsert
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Upsert
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Upsert
    suspend fun insertResources(resources: List<MediaResourceEntity>)

    @Transaction
    suspend fun insertMedia(
        media: List<MediaEntity>,
        items: List<MediaItemEntity>,
        tracks: List<TrackEntity>,
        resources: List<MediaResourceEntity>
    ) {
        insertRawMedia(media)
        insertMediaItems(items)
        insertTracks(tracks)
        insertResources(resources)
    }

    @Query("UPDATE MediaEntity SET previewCoverUrl = :previewUrl, fullCoverUrl = :largeUrl WHERE id = :mediaId")
    suspend fun updateMediaCovers(mediaId: String, previewUrl: String?, largeUrl: String?)
}