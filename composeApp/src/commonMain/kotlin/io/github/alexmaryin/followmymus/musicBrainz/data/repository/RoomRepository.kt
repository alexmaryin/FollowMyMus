package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistRelationsDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.TransactionalDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.ArtistEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import org.koin.core.annotation.Factory

@Factory(binds = [LocalDbRepository::class])
class RoomRepository(
    private val transactionalDao: TransactionalDao,
    private val relationDao: ArtistRelationsDao,
    private val artistDao: ArtistDao
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

    override suspend fun clearLocalData() {
        relationDao.clearTags()
        artistDao.clearArtists()
    }
}