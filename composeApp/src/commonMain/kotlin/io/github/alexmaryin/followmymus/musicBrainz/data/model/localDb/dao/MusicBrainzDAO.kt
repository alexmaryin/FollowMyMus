package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.*
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicBrainzDAO {

    // Select entities from DB in Flows

    @Query("SELECT * FROM ResourceEntity WHERE artistId = :artistId")
    fun getArtistResources(artistId: String): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM ReleaseEntity WHERE artistId = :artistId")
    fun getArtistReleases(artistId: String): Flow<List<ReleaseEntity>>

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true")
    fun getFavoriteArtists(): Flow<List<ArtistWithRelations>>

    @Transaction
    @Query("SELECT EXISTS(SELECT 1 FROM ArtistEntity WHERE id = :artistId)")
    suspend fun isArtistExists(artistId: String): Boolean

    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true")
    fun getFavoriteArtistsIds(): Flow<List<String>>

    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true AND syncStatus = 'PendingRemoteAdd'")
    suspend fun getIdsToPushAsList(): List<String>

    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true")
    suspend fun getIdsAsList(): List<String>

    @Query("SELECT id FROM ArtistEntity WHERE syncStatus = 'PendingRemoteRemove'")
    suspend fun getArtistsIdsPendingRemove(): List<String>


    // Updates and meta data from DB

    @Query("SELECT EXISTS(SELECT * FROM ArtistEntity WHERE syncStatus <> 'OK')")
    suspend fun hasPendingActions(): Boolean

    @Query("UPDATE ArtistEntity SET syncStatus = 'OK' WHERE id IN (:ids)")
    suspend fun markFavoriteArtistsAsSynced(ids: Set<String>)

    @Query("UPDATE ArtistEntity SET syncStatus = :newStatus, isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateArtistSyncStatus(id: String, newStatus: SyncStatus, isFavorite: Boolean = true)

    @Query("UPDATE ReleaseEntity SET previewCoverUrl = :previewCoverUrl, fullCoverUrl = :fullCoverUrl WHERE id = :id")
    suspend fun updateReleaseCovers(id: String, previewCoverUrl: String?, fullCoverUrl: String?)



    // INSERTS

    @Upsert
    suspend fun insertArtist(artist: ArtistEntity)

    @Upsert
    suspend fun insertArea(area: AreaEntity)

    @Upsert
    suspend fun insertTags(tags: List<TagEntity>)

    @Upsert
    suspend fun insertResources(resources: List<ResourceEntity>)

    @Upsert
    suspend fun insertReleases(releases: List<ReleaseEntity>)

    @Transaction
    suspend fun insertDetailsForArtist(resources: List<ResourceEntity>, releases: List<ReleaseEntity>) {
        insertResources(resources)
        insertReleases(releases)
    }

    @Transaction
    suspend fun insertArtist(
        artist: ArtistEntity,
        area: AreaEntity?,
        beginArea: AreaEntity?,
        tags: List<TagEntity>
    ) {
        insertArtist(artist)
        area?.let { insertArea(it) }
        beginArea?.let { insertArea(it) }
        insertTags(tags)
    }

    @Transaction
    suspend fun insertFavoriteArtist(
        artist: ArtistEntity,
        area: AreaEntity?,
        beginArea: AreaEntity?,
        tags: List<TagEntity>
    ) = insertArtist(artist.copy(isFavorite = true), area, beginArea, tags)

    @Transaction
    suspend fun bulkInsertArtistsDto(artists: List<ArtistDto>) {
        artists.forEach { artist ->
            insertFavoriteArtist(
                artist = artist.toEntity(true, SyncStatus.OK),
                area = artist.area?.toEntity(),
                beginArea = artist.beginArea?.toEntity(),
                tags = artist.tags.map { tag -> tag.toEntity(artist.id) }
            )
        }
    }



    // DELETE

    @Query("DELETE FROM ArtistEntity")
    suspend fun clearArtists()

    @Query("DELETE FROM TagEntity")
    suspend fun clearTags()

    @Query("DELETE FROM ResourceEntity WHERE artistId = :artistId")
    suspend fun clearResources(artistId: String)

    @Query("DELETE FROM ReleaseEntity WHERE artistId = :artistId")
    suspend fun clearReleases(artistId: String)


    @Query("DELETE FROM ArtistEntity WHERE id = :id")
    suspend fun deleteArtistById(id: String)

    @Query("DELETE FROM ArtistEntity WHERE id IN (:ids)")
    suspend fun bulkDeleteArtistsById(ids: List<String>)

    @Query("UPDATE ArtistEntity SET isFavorite = false WHERE id = :id")
    suspend fun removeArtistFromFavorites(id: String)
}