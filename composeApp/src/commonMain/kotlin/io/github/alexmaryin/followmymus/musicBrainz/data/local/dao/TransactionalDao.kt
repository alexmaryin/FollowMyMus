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
        relationsDao.insertTags(tags)
        area?.let { relationsDao.insertArea(it) }
        beginArea?.let { relationsDao.insertArea(it) }
    }

    @Transaction
    suspend fun bulkInsertArtistsWithRelations(
        artists: List<ArtistEntity>,
        areas: List<AreaEntity>,
        tags: List<TagEntity>,
        relationsDao: ArtistRelationsDao,
        artistDao: ArtistDao
    ) {
        artistDao.insertArtists(artists)
        relationsDao.insertTags(tags)
        relationsDao.insertAreas(areas)
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
