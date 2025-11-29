package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ReleaseDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.dao.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.collections.AtomicMutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [ReleasesRepository::class])
class ApiReleasesRepository(
    private val searchEngine: SearchEngine,
    private val dao: MusicBrainzDAO
) : ReleasesRepository {

    override fun getArtistReleases(artistId: String) = dao.getArtistReleases(artistId)
        .map { it.groupByPrimary() }

    override fun getArtistResources(artistId: String) = dao.getArtistResources(artistId)
        .map { it.groupByType() }

    @OptIn(SupabaseInternal::class)
    override suspend fun syncReleases(artistId: String) {
        val releasesResult = searchEngine.searchReleases(artistId)
        releasesResult.forSuccess { artistDto ->
            val resources = artistDto.resources
            val releases = artistDto.releases
            dao.insertDetailsForArtist(
                resources = resources.map { it.toEntity(artistDto.id) },
                releases = releases.map { it.toEntity(artistDto.id) }
            )
            val releasesList = AtomicMutableList<ReleaseDto>()
            releasesList.addAll(releases)
            withContext(Dispatchers.IO) {
                releasesList.forEachIndexed { index, release ->
                    launch {
                        val coverResult = searchEngine.searchCovers(release.id)
                        coverResult.forSuccess { coverDto ->
                            releasesList[index] = release.copy(coverImages = coverDto.images)
                            val previewCover = coverDto.images.selectCover { selectPreview() }
                            val fullCover = coverDto.images.selectCover { url }
                            dao.updateReleaseCovers(release.id, previewCover, fullCover)
                        }
                    }
                }
            }
        }
    }

    override suspend fun clearDetails(artistId: String) {
        dao.clearResources(artistId)
        dao.clearReleases(artistId)
    }
}