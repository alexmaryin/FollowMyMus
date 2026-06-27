package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.PagingSource
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.core.UndefinedError
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.MediaDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ReleasesDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ResourceDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.TransactionalDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.CoverImageDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.RecordingDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ResourceDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.UrlDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.CoverArtResponse
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchMediaResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine

/**
 * In-memory fakes for the paging repository tests.
 * They record every call so tests can assert on the
 * (offset, limit) sequence and on the inserted entities.
 */

class FakePagingSearchEngine : SearchEngine {

    data class ReleasesCall(val offset: Int, val limit: Int)
    data class MediaCall(val offset: Int, val limit: Int)

    val releasesCalls = mutableListOf<ReleasesCall>()
    val mediaCalls = mutableListOf<MediaCall>()

    val releasesQueue: ArrayDeque<Result<ArtistDto>> = ArrayDeque()
    val mediaQueue: ArrayDeque<Result<SearchMediaResponse>> = ArrayDeque()

    fun enqueueReleases(vararg responses: Result<ArtistDto>) {
        responses.forEach { releasesQueue.addLast(it) }
    }

    fun enqueueMedia(vararg responses: Result<SearchMediaResponse>) {
        responses.forEach { mediaQueue.addLast(it) }
    }

    override suspend fun searchArtists(query: String, offset: Int, limit: Int): io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchArtistResponse =
        error("not used in paging tests")

    override fun getArtistFromCache(artistId: String): ArtistDto? = null

    override suspend fun fetchArtistsById(ids: List<String>): Result<List<ArtistDto>> =
        error("not used in paging tests")

    override suspend fun searchReleases(artistId: String, offset: Int, limit: Int): Result<ArtistDto> {
        releasesCalls += ReleasesCall(offset, limit)
        return releasesQueue.removeFirstOrNull()
            ?: error("FakePagingSearchEngine: no queued release response for offset=$offset")
    }

    override suspend fun searchMedia(releaseId: String, offset: Int, limit: Int): Result<SearchMediaResponse> {
        mediaCalls += MediaCall(offset, limit)
        return mediaQueue.removeFirstOrNull()
            ?: error("FakePagingSearchEngine: no queued media response for offset=$offset")
    }
}

/**
 * A search engine that suspends indefinitely starting from a configured
 * release or media call index. Used to test cooperative cancellation:
 * the test can cancel the running coroutine while it's blocked on the
 * gate and then resume the gate to let any cancelled continuations
 * throw CancellationException cleanly.
 */
class GatedPagingSearchEngine(
    private val base: FakePagingSearchEngine = FakePagingSearchEngine(),
    val pauseOnReleaseCall: Int = 1,
    val pauseOnMediaCall: Int = 1
) : SearchEngine by base {

    val releaseGate = kotlinx.coroutines.CompletableDeferred<Unit>()
    val mediaGate = kotlinx.coroutines.CompletableDeferred<Unit>()

    override suspend fun searchReleases(artistId: String, offset: Int, limit: Int): Result<ArtistDto> {
        if (base.releasesCalls.size + 1 >= pauseOnReleaseCall) {
            releaseGate.await()
        }
        return base.searchReleases(artistId, offset, limit)
    }

    override suspend fun searchMedia(releaseId: String, offset: Int, limit: Int): Result<SearchMediaResponse> {
        if (base.mediaCalls.size + 1 >= pauseOnMediaCall) {
            mediaGate.await()
        }
        return base.searchMedia(releaseId, offset, limit)
    }
}

class FakeCoversEngine : CoversEngine {
    val calls = mutableListOf<String>()

    private fun success(mediaId: String) = Result.Success(
        CoverArtResponse(
            images = listOf(
                CoverImageDto(
                    type = io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ImageType.FRONT,
                    url = "http://covers/$mediaId/front.jpg",
                    thumbnails = emptyList()
                )
            ),
            release = mediaId
        )
    )

    override suspend fun getReleaseCovers(releaseId: String): Result<CoverArtResponse> {
        calls += releaseId
        return success(releaseId)
    }

    override suspend fun getMediaCovers(mediaId: String): Result<CoverArtResponse> {
        calls += mediaId
        return success(mediaId)
    }
}

class RecordingReleasesDao : ReleasesDao {
    val insertedReleases = mutableListOf<ReleaseEntity>()
    val coverUpdates = mutableListOf<Triple<String, String?, String?>>()

    override fun getArtistReleases(artistId: String) =
        error("not used in paging tests")

    override fun getPagedArtistReleases(artistId: String): PagingSource<Int, ReleaseEntity> =
        error("not used in paging tests")

    override fun getArtistReleasesCount(artistId: String) =
        error("not used in paging tests")

    override suspend fun insertReleases(releases: List<ReleaseEntity>) {
        insertedReleases += releases
    }

    override suspend fun updateReleaseCovers(id: String, preview: String?, full: String?) {
        coverUpdates += Triple(id, preview, full)
    }

    override suspend fun clearReleases(artistId: String) {
        insertedReleases.clear()
    }

    override suspend fun clear() = Unit
}

class RecordingResourceDao : ResourceDao {
    val insertedResources = mutableListOf<ResourceEntity>()

    override fun getArtistResources(artistId: String) =
        error("not used in paging tests")

    override suspend fun insertResources(resources: List<ResourceEntity>) {
        insertedResources += resources
    }

    override suspend fun clearResources(artistId: String) = Unit

    override suspend fun clear() = Unit
}

class RecordingTransactionalDao : TransactionalDao {
    val calls = mutableListOf<Pair<List<ResourceEntity>, List<ReleaseEntity>>>()

    override suspend fun insertDetails(
        resources: List<ResourceEntity>,
        releases: List<ReleaseEntity>,
        resourceDao: ResourceDao,
        releasesDao: ReleasesDao
    ) {
        calls += resources to releases
        resourceDao.insertResources(resources)
        releasesDao.insertReleases(releases)
    }

    override suspend fun insertArtistWithRelations(
        artist: ArtistEntity,
        area: AreaEntity?,
        beginArea: AreaEntity?,
        tags: List<TagEntity>,
        relationsDao: io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistRelationsDao,
        artistDao: io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
    ) = error("not used in paging tests")

    override suspend fun bulkInsertArtistsWithRelations(
        artists: List<ArtistEntity>,
        areas: List<AreaEntity>,
        tags: List<TagEntity>,
        relationsDao: io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistRelationsDao,
        artistDao: io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
    ) = error("not used in paging tests")
}

class RecordingMediaDao : MediaDao {
    val insertedMedia = mutableListOf<MediaEntity>()
    val insertedItems = mutableListOf<MediaItemEntity>()
    val insertedTracks = mutableListOf<TrackEntity>()
    val insertedResources = mutableListOf<MediaResourceEntity>()
    val coverUpdates = mutableListOf<Triple<String, String?, String?>>()

    override fun getReleaseMedia(releaseId: String) =
        error("not used in paging tests")

    override fun getPagedReleaseMedia(releaseId: String): PagingSource<Int, MediaWithData> =
        error("not used in paging tests")

    override fun getMediaCount(releaseId: String) =
        error("not used in paging tests")

    override suspend fun insertRawMedia(media: List<MediaEntity>) {
        insertedMedia += media
    }

    override suspend fun insertMediaItems(items: List<MediaItemEntity>) {
        insertedItems += items
    }

    override suspend fun insertTracks(tracks: List<TrackEntity>) {
        insertedTracks += tracks
    }

    override suspend fun insertResources(resources: List<MediaResourceEntity>) {
        insertedResources += resources
    }

    override suspend fun insertMedia(
        media: List<MediaEntity>,
        items: List<MediaItemEntity>,
        tracks: List<TrackEntity>,
        resources: List<MediaResourceEntity>
    ) {
        insertRawMedia(media)
        insertMediaItems(items)
        insertTracks(tracks)
        insertResources(resources)
    }

    override suspend fun updateMediaCovers(mediaId: String, previewUrl: String?, largeUrl: String?) {
        coverUpdates += Triple(mediaId, previewUrl, largeUrl)
    }

    override suspend fun clearMedia() = Unit
}

// ---- DTO builders used by tests ----

fun releaseDto(index: Int) = io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ReleaseDto(
    id = "rel-$index",
    title = "Release $index",
    firstReleaseDate = null
)

fun artistDtoWith(index: Int, count: Int) = ArtistDto(
    id = "artist-1",
    type = null,
    score = null,
    name = "Test Artist",
    sortName = "Test Artist",
    area = null,
    beginArea = null,
    disambiguation = null,
    lifeSpan = null,
    releaseCount = count,
    releases = List(index) { releaseDto(it) }
)

fun successReleases(index: Int, count: Int) = Result.Success(artistDtoWith(index, count))

fun errorResult(message: String? = null) = Result.Error(UndefinedError, message)

fun mediaDto(index: Int, trackCount: Int = 1) =
    io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaDto(
        id = "med-$index",
        title = "Media $index",
        items = listOf(
            io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaItemDto(
                id = "item-$index",
                position = 1,
                format = "CD",
                title = "Disc $index",
                trackCount = trackCount,
                trackOffset = 0,
                tracks = (1..trackCount).map { i ->
                    io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.TrackDto(
                        id = "track-$index-$i",
                        position = i,
                        recording = RecordingDto(title = "Track $i", length = 1000L, disambiguation = null)
                    )
                }
            )
        ),
        resources = listOf(
            ResourceDto(type = "streaming", url = UrlDto(id = "r-$index", resource = "https://stream/$index"))
        )
    )

fun mediaResponseWith(index: Int, count: Int, trackCount: Int = 1) = Result.Success(
    SearchMediaResponse(
        releases = List(index) { mediaDto(it, trackCount) },
        count = count,
        offset = 0
    )
)

fun emptyMediaResponse() = Result.Success(SearchMediaResponse(releases = emptyList(), count = 0, offset = 0))
