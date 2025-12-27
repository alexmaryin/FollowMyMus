package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ReleasesDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ResourceDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.TransactionalDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.httpsReplace
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single(binds = [ReleasesRepository::class])
class ApiReleasesRepository(
    private val searchEngine: SearchEngine,
    private val coversEngine: CoversEngine,
    private val releaseDao: ReleasesDao,
    private val resourceDao: ResourceDao,
    private val transactionalDao: TransactionalDao
) : ReleasesRepository {

    override val workState = MutableStateFlow(WorkState.IDLE)

    private val _errors = Channel<ErrorType>()
    override val errors = _errors.receiveAsFlow()

    override fun getArtistReleases(artistId: String) = releaseDao.getArtistReleases(artistId)
        .map { it.groupByCategories() }

    override fun getArtistResources(artistId: String) = resourceDao.getArtistResources(artistId)
        .map { it.groupByType() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun syncReleases(artistId: String) {

        workState.update { WorkState.LOADING }

        val releasesResult = searchEngine.searchReleases(artistId)
        releasesResult.forSuccess { artistDto ->
            transactionalDao.insertDetails(
                resources = artistDto.resources.map { it.toEntity(artistDto.id) },
                releases = artistDto.releases.map { it.toEntity(artistDto.id) },
                resourceDao = resourceDao,
                releasesDao = releaseDao
            )

            workState.update { WorkState.COVERING }

            artistDto.releases
                .sortedWith(compareByDescending { it.firstReleaseDate })
                .asFlow()
                .flatMapMerge(concurrency = 25) { release ->
                    flow {
                        emit(coversEngine.getReleaseCovers(release.id) to release)
                    }
                }
                .collect { (result, release) ->
                    result.forSuccess { coverDto ->
                        val previewUrl = coverDto.images.selectCover { selectPreview() }?.httpsReplace()
                        val largeUrl = coverDto.images.selectCover { url }?.httpsReplace()
                        releaseDao.updateReleaseCovers(release.id, previewUrl, largeUrl)
                    }
                    result.forError { error ->
                        _errors.send(error.type)
                    }
                }

            workState.update { WorkState.IDLE }
        }
        releasesResult.forError { error ->
            _errors.send(error.type)
            workState.update { WorkState.IDLE }
        }
    }

    override suspend fun clearDetails(artistId: String) {
        resourceDao.clearResources(artistId)
        releaseDao.clearReleases(artistId)
    }
}
