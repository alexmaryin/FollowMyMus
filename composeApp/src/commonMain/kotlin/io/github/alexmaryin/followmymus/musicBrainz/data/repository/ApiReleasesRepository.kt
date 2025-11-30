package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.dao.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single(binds = [ReleasesRepository::class])
class ApiReleasesRepository(
    private val searchEngine: SearchEngine,
    private val dao: MusicBrainzDAO
) : ReleasesRepository {

    override val workState = MutableStateFlow(WorkState.IDLE)

    override val errors = MutableSharedFlow<ErrorType>(replay = 1)

    override fun getArtistReleases(artistId: String) = dao.getArtistReleases(artistId)
        .map { it.groupByPrimary() }

    override fun getArtistResources(artistId: String) = dao.getArtistResources(artistId)
        .map { it.groupByType() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun syncReleases(artistId: String) {

        workState.update { WorkState.LOADING }

        val releasesResult = searchEngine.searchReleases(artistId)
        releasesResult.forSuccess { artistDto ->
            dao.insertDetailsForArtist(
                resources = artistDto.resources.map { it.toEntity(artistDto.id) },
                releases = artistDto.releases.map { it.toEntity(artistDto.id) }
            )

            workState.update { WorkState.COVERING }

            artistDto.releases
                .sortedWith(compareByDescending { it.firstReleaseDate })
                .asFlow()
                .flatMapMerge(concurrency = 5) { release ->
                    flow {
                        emit(searchEngine.searchCovers(release.id) to release)
                    }
                }
                .collect { (result, release) ->
                    result.forSuccess { coverDto ->
                        val previewUrl = coverDto.images.selectCover { selectPreview() }?.httpsReplace()
                        val largeUrl = coverDto.images.selectCover { url }?.httpsReplace()
                        dao.updateReleaseCovers(release.id, previewUrl, largeUrl)
                    }
                    result.forError { error ->
                        errors.emit(error.type)
                    }
                }

            workState.update { WorkState.IDLE }
        }
        releasesResult.forError { error ->
            errors.emit(error.type)
            workState.update { WorkState.IDLE }
        }
    }

    override suspend fun clearDetails(artistId: String) {
        dao.clearResources(artistId)
        dao.clearReleases(artistId)
    }
}

private fun String.httpsReplace() = replace("http://", "https://")