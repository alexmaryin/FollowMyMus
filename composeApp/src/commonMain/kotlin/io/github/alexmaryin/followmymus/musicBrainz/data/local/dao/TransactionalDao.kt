package io.github.alexmaryin.followmymus.musicBrainz.data.local.dao

import androidx.room.Dao
import androidx.room.Transaction
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*

@Dao
interface TransactionalDao {

    @Transaction
    suspend fun insertArtistWithRelations(
        artist: ArtistEntity,
        area: AreaEntity?,
        beginArea: AreaEntity?,
        tags: List<TagEntity>,
        relationsDao: ArtistRelationsDao,
        artistDao: ArtistDao
    ) {
        artistDao.insertArtist(artist)
        area?.let { relationsDao.insertArea(it) }
        beginArea?.let { relationsDao.insertArea(it) }
        relationsDao.insertTags(tags)
    }

    @Transaction
    suspend fun insertDetails(
        resources: List<ResourceEntity>,
        releases: List<ReleaseEntity>,
        resourceDao: ResourceDao,
        releasesDao: ReleasesDao
    ) {
        resourceDao.insertResources(resources)
        releasesDao.insertReleases(releases)
    }
}
