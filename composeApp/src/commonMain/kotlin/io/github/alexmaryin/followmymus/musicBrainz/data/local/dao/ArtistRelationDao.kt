package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.AreaEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.TagEntity

@Dao
interface ArtistRelationsDao {

    @Upsert
    suspend fun insertArea(area: AreaEntity)

    @Upsert
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM TagEntity")
    suspend fun clearTags()
}
