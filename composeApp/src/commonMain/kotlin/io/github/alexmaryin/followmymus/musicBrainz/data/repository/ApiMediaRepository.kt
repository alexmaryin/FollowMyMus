package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.map
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.core.paging.RoomPagingCount
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.MediaDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaDto
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.httpsReplace
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.MediaRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single(binds = [MediaRepository::class])
class ApiMediaRepository(
    private val searchEngine: SearchEngine,
    private val coversEngine: CoversEngine,
    private val mediaDao: MediaDao
) : MediaRepository {
    override val workState = MutableStateFlow(WorkState.IDLE)

    private val _errors = Channel<ErrorType>()
    override val errors = _errors.receiveAsFlow()

    private val _totalNetworkMedia = MutableStateFlow<Int?>(null)
    override val totalNetworkMedia: Flow<Int?> = _totalNetworkMedia.asStateFlow()

    override fun getReleaseMedia(releaseId: String) = Pager(
        config = PagingDefaults.roomConfig(),
        pagingSourceFactory = { mediaDao.getPagedReleaseMedia(releaseId) }
    ).flow.map { paging -> paging.map { it.toMedia() } }

    override fun getMediaCount(releaseId: String): Flow<Int?> =
        RoomPagingCount { mediaDao.getMediaCount(releaseId) }.flow

    private suspend fun persistPage(releaseId: String, releases: List<MediaDto>) {
        mediaDao.insertMedia(
            media = releases.map { it.toEntity(releaseId) },
            items = releases.flatMap { media ->
                media.items.map { item -> item.toEntity(media.id) }
            },
            tracks = releases.flatMap { media ->
                media.items.flatMap { item ->
                    item.tracks.map { track -> track.toEntity(media.id, item.id) }
                }
            },
            resources = releases.flatMap { media ->
                media.resources.map { it.toMediaResourceEntity(media.id) }
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchPageCovers(releases: List<MediaDto>) {
        releases
            .asFlow()
            .flatMapMerge(concurrency = PagingDefaults.API_PAGE) { media ->
                flow { emit(coversEngine.getMediaCovers(media.id) to media) }
            }
            .collect { (result, media) ->
                result.forSuccess { coverDto ->
                    val previewUrl = coverDto.images.selectCover { selectPreview() }?.httpsReplace()
                    val largeUrl = coverDto.images.selectCover { url }?.httpsReplace()
                    mediaDao.updateMediaCovers(media.id, previewUrl, largeUrl)
                }
                result.forError { error ->
                    _errors.send(error.type)
                }
            }
    }

    override suspend fun fetchReleasesMedia(releaseId: String) {

        workState.update { WorkState.LOADING }
        _totalNetworkMedia.value = null

        var offset = 0
        var pageIndex = 1
        var partialSync = false
        var totalCount = 0

        pageLoop@ while (_totalNetworkMedia.value == null
            || (offset < totalCount && pageIndex <= PagingDefaults.MAX_MEDIA_PAGES)
        ) {
            currentCoroutineContext().ensureActive()

            val response = searchEngine.searchMedia(releaseId, offset, PagingDefaults.API_PAGE)

            response.forError { error ->
                _errors.send(error.type)
                if (offset != 0) partialSync = true
                workState.update { if (offset != 0) WorkState.PARTIAL_SYNC else WorkState.IDLE }
                break@pageLoop
            }

            response.forSuccess { mediaResponse ->
                if (mediaResponse.releases.isEmpty()) break@pageLoop

                if (totalCount == 0) {
                    totalCount = mediaResponse.count.coerceAtLeast(mediaResponse.releases.size)
                    _totalNetworkMedia.value = totalCount
                }

                persistPage(releaseId, mediaResponse.releases)
                fetchPageCovers(mediaResponse.releases)
                offset += mediaResponse.releases.size
                pageIndex++

                if (offset >= totalCount) break@pageLoop
            }
        }

        if (offset < totalCount) partialSync = true

        workState.update { if (partialSync) WorkState.PARTIAL_SYNC else WorkState.IDLE }
    }
}
