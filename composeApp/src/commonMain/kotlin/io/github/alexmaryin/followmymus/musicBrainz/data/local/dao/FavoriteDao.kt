package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY sortName COLLATE NOCASE")
    fun getArtistsSortedByAbc(): Flow<List<ArtistWithRelations>>

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY country, sortName COLLATE NOCASE")
    fun getArtistsSortedByCountry(): Flow<List<ArtistWithRelations>>

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY type")
    fun getArtistsSortedByType(): Flow<List<ArtistWithRelations>>

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY createdAt DESC")
    fun getArtistsSortedByDate(): Flow<List<ArtistWithRelations>>

    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true")
    fun getArtists(): Flow<List<ArtistWithRelations>>

    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true")
    fun getFavoriteArtistIds(): Flow<List<String>>
}
