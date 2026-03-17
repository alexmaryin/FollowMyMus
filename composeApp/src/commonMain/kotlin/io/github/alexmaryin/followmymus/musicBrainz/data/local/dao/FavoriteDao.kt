package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY sortName COLLATE NOCASE")
    fun getPagedArtistsSortedByAbc(): PagingSource<Int, ArtistWithRelations>

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY country, sortName COLLATE NOCASE")
    fun getPagedArtistsSortedByCountry(): PagingSource<Int, ArtistWithRelations>

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY type")
    fun getPagedArtistsSortedByType(): PagingSource<Int, ArtistWithRelations>

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true ORDER BY createdAt DESC")
    fun getPagedArtistsSortedByDate(): PagingSource<Int, ArtistWithRelations>

    @Transaction
    @Query("SELECT * FROM ArtistEntity WHERE isFavorite = true")
    fun getPagedArtists(): PagingSource<Int, ArtistWithRelations>

    @Transaction
    @Query("SELECT id FROM ArtistEntity WHERE isFavorite = true")
    fun getFavoriteArtistsIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM ArtistEntity WHERE isFavorite = true")
    fun getTotalCount(): Flow<Int>
}
