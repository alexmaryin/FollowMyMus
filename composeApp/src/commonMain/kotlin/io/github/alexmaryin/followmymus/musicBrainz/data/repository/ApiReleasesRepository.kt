package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.map
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.core.paging.RoomPagingCount
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ReleasesDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ResourceDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.TransactionalDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ReleaseDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ResourceDto
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.httpsReplace
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

    private val _totalNetworkReleases = MutableStateFlow<Int?>(null)
    override val totalNetworkReleases: Flow<Int?> = _totalNetworkReleases.asStateFlow()

    override fun getArtistReleases(artistId: String) = Pager(
        config = PagingDefaults.roomConfig(),
        pagingSourceFactory = { releaseDao.getPagedArtistReleases(artistId) }
    ).flow.map { paging -> paging.map { it.toRelease() } }

    override fun getArtistReleasesCount(artistId: String): Flow<Int?> =
        RoomPagingCount {
            releaseDao.getArtistReleasesCount(artistId)
        }.flow

    override fun getArtistResources(artistId: String) = resourceDao.getArtistResources(artistId)
        .map { it.groupByType() }

    private suspend fun persistPage(
        artistId: String,
        releases: List<ReleaseDto>,
        resources: List<ResourceDto>
    ) {
        transactionalDao.insertDetails(
            resources = resources.map { it.toEntity(artistId) },
            releases = releases.map { it.toEntity(artistId) },
            resourceDao = resourceDao,
            releasesDao = releaseDao
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchPageCovers(releases: List<ReleaseDto>) {
        releases
            .asFlow()
            .flatMapMerge(concurrency = PagingDefaults.API_PAGE) { release ->
                flow { emit(coversEngine.getReleaseCovers(release.id) to release) }
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
    }

    override suspend fun syncReleases(artistId: String) {

        workState.update { WorkState.LOADING }
        _totalNetworkReleases.value = null

        var offset = 0
        var pageIndex = 1
        var partialSync = false
        var totalCount = 0

        pageLoop@ while (_totalNetworkReleases.value == null
            || (offset < totalCount && pageIndex <= PagingDefaults.MAX_RELEASE_PAGES)
        ) {
            currentCoroutineContext().ensureActive()

            val response = searchEngine.searchReleases(artistId, offset, PagingDefaults.API_PAGE)

            response.forError { error ->
                _errors.send(error.type)
                if (offset != 0) partialSync = true
                workState.update { if (offset != 0) WorkState.PARTIAL_SYNC else WorkState.IDLE }
                break@pageLoop
            }

            response.forSuccess { artistDto ->
                if (artistDto.releases.isEmpty()) break@pageLoop

                if (totalCount == 0) {
                    totalCount = artistDto.releaseCount.coerceAtLeast(artistDto.releases.size)
                    _totalNetworkReleases.value = totalCount
                }

                persistPage(artistId, artistDto.releases, artistDto.resources)
                fetchPageCovers(artistDto.releases)
                offset += artistDto.releases.size
                pageIndex++

                if (offset >= totalCount) break@pageLoop
            }
        }

        if (offset < totalCount) partialSync = true

        workState.update { if (partialSync) WorkState.PARTIAL_SYNC else WorkState.IDLE }
    }

    override suspend fun clearDetails(artistId: String) {
        resourceDao.clearResources(artistId)
        releaseDao.clearReleases(artistId)
    }
}
