package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import org.koin.core.annotation.Factory

@Factory(binds = [LocalDbRepository::class])
class RoomRepository(
    private val transactionalDao: TransactionalDao,
    private val relationDao: ArtistRelationsDao,
    private val artistDao: ArtistDao,
    private val releasesDao: ReleasesDao,
    private val resourceDao: ResourceDao
) : LocalDbRepository {

    override suspend fun insertArtist(artist: ArtistDto, transform: ArtistDto.() -> ArtistEntity) {
        transactionalDao.insertArtistWithRelations(
            artist = transform(artist),
            area = artist.area?.toEntity(),
            beginArea = artist.beginArea?.toEntity(),
            tags = artist.tags.map { tag -> tag.toEntity(artist.id) },
            relationsDao = relationDao,
            artistDao = artistDao
        )
    }

    override suspend fun bulkInsertArtists(artists: List<ArtistDto>, transform: ArtistDto.() -> ArtistEntity) {
        val artistEntities = artists.map { transform(it) }
        val areasEntities = artists.map {
            listOfNotNull(it.area?.toEntity(), it.beginArea?.toEntity())
        }.flatten()
        val tagsEntities = artists.map { artist ->
            artist.tags.map { tag -> tag.toEntity(artist.id) }
        }.flatten()
        transactionalDao.bulkInsertArtistsWithRelations(
            artists = artistEntities,
            areas = areasEntities,
            tags = tagsEntities,
            relationsDao = relationDao,
            artistDao = artistDao
        )
    }

    override suspend fun clearLocalData() {
        relationDao.clearTags()
        relationDao.clearAreas()
        releasesDao.clear()
        resourceDao.clear()
        artistDao.clearArtists()
    }
}