package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicBrainzDAO {

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true")
    fun getFavoriteArtists(): Flow<List<ArtistWithRelations>>

    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true")
    suspend fun getFavoriteArtistsIds(): List<String>

    @Query("SELECT * FROM ArtistEntity WHERE id = :id")
    suspend fun getArtistById(id: String): ArtistWithRelations?

    @Upsert
    suspend fun insertArtist(artist: ArtistEntity)

    @Upsert
    suspend fun insertArea(area: AreaEntity)

    @Upsert
    suspend fun insertTags(tags: List<TagEntity>)

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

    @Query("DELETE FROM ArtistEntity")
    suspend fun clearArtists()

    @Query("DELETE FROM ArtistEntity WHERE id = :id")
    suspend fun deleteArtistById(id: String)

    @Query("UPDATE ArtistEntity SET isFavorite = false WHERE id = :id")
    suspend fun removeArtistFromFavorites(id: String)
}