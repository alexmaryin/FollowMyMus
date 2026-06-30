package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.network.RateLimitedApiQueue
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.NewReleasesDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseState
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.selectCover
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.selectPreview
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ReleaseGroupDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.BrainzApiError
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.httpsReplace
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.NewReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.preferences.PreferenceSource
import io.github.alexmaryin.followmymus.preferences.getAppSettings
import io.github.alexmaryin.followmymus.preferences.setNewReleasesFloor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

@Single(binds = [NewReleasesRepository::class])
class ApiNewReleasesRepository(
    private val searchEngine: SearchEngine,
    private val coversEngine: CoversEngine,
    private val rateLimitedApiQueue: RateLimitedApiQueue,
    private val newReleasesDao: NewReleasesDao,
    private val favoriteDao: FavoriteDao,
    private val preferenceSource: PreferenceSource
) : NewReleasesRepository {

    private val pager = Pager(
        config = PagingDefaults.roomConfig(),
        pagingSourceFactory = { newReleasesDao.getNewReleases() },
    )

    private val _workState = MutableStateFlow(WorkState.IDLE)
    override val workState: StateFlow<WorkState> = _workState

    private val _errors = MutableSharedFlow<ErrorType>(extraBufferCapacity = 16)
    override val errors: Flow<ErrorType> = _errors.asSharedFlow()

    override fun getNewReleases(): Flow<PagingData<NewReleaseEntity>> = pager.flow

    override suspend fun syncNewReleases(): Result<Unit> {
        _workState.value = WorkState.LOADING
        return try {
            val (dateFloor, favoriteIds) = readSyncInputs()
            syncBatches(dateFloor, favoriteIds)
            Result.Success(Unit)
        } catch (e: CancellationException) {
            _workState.value = WorkState.IDLE
            throw e
        } catch (e: Exception) {
            val err = BrainzApiError.Unknown(e.message)
            _errors.tryEmit(err)
            _workState.value = WorkState.PARTIAL_SYNC
            Result.Error(err, e.message)
        }
    }

    private suspend fun readSyncInputs(): Pair<LocalDate, List<String>> =
        combine(
            preferenceSource.getAppSettings(),
            favoriteDao.getFavoriteArtistsIds(),
        ) { settings, ids ->
            val rawFloor = settings.newReleasesLastOpenedDay
                ?.plus(1, DateTimeUnit.DAY)
                ?: today().minus(30, DateTimeUnit.DAY)
            val floor = rawFloor.coerceAtMost(today())
            floor to ids
        }.first()

    private suspend fun syncBatches(
        dateFloor: LocalDate,
        favoriteIds: List<String>,
    ) {
        // Empty case: early return with no API call, no DB write.
        favoriteIds.takeUnless(List<String>::isNotEmpty)?.let {
            _workState.value = WorkState.IDLE
            return
        }

        val favoriteIdsSet = favoriteIds.toSet()

        val partial = favoriteIds.chunked(BATCH_SIZE).fold(false) { partial, batch ->
            currentCoroutineContext().ensureActive()
            fetchAndPersistBatch(batch, favoriteIdsSet, dateFloor).forError { error ->
                _errors.tryEmit(error.type)
                return@fold true
            }
            partial
        }

        val postState = if (partial) WorkState.PARTIAL_SYNC else WorkState.IDLE
        _workState.also { it.value = postState }
        if (!partial) preferenceSource.setNewReleasesFloor(today(), nowInstant())
    }

    private suspend fun fetchAndPersistBatch(
        batch: List<String>,
        favoriteIdsSet: Set<String>,
        floor: LocalDate,
    ): Result<Unit> {
        val query = buildQuery(floor, batch)
        var offset = 0
        updateLoop@ while (offset < PagingDefaults.MAX_RELEASE_PAGES * PagingDefaults.MAX_SIZE) {
            currentCoroutineContext().ensureActive()
            val pageResult = rateLimitedApiQueue.enqueue {
                searchEngine.searchReleaseGroups(query, offset, PagingDefaults.API_PAGE)
            }
            pageResult.forSuccess { page ->
                if (page.releaseGroups.isEmpty()) break@updateLoop
                newReleasesDao.upsertNewReleases(
                    page.releaseGroups.toNewReleaseEntities(favoriteIdsSet, nowInstant()),
                )
                fetchPageCovers(page.releaseGroups)
                offset += PagingDefaults.API_PAGE
                if (offset >= page.count) break@updateLoop
            }
            pageResult.forError { error -> return error }
        }
        return Result.Success(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchPageCovers(releases: List<ReleaseGroupDto>) {
        releases
            .asFlow()
            .flatMapMerge(concurrency = PagingDefaults.API_PAGE) { release ->
                flow { emit(coversEngine.getReleaseCovers(release.id) to release) }
            }
            .collect { (result, release) ->
                result.forSuccess { coverDto ->
                    val previewUrl = coverDto.images.selectCover { selectPreview() }?.httpsReplace()
                    newReleasesDao.updateCoverFrontUrl(release.id, previewUrl)
                }
                result.forError { error ->
                    _errors.tryEmit(error.type)
                }
            }
    }

    /**
     * The search index field is `firstreleasedate` (no hyphens); the response
     * field on the wire is `first-release-date` (kebab-case). Only the index
     * field is legal in the `query=` parameter.
     */
    private fun buildQuery(
        floor: LocalDate,
        batch: List<String>,
    ): String = batch.joinToString(
        prefix = "firstreleasedate:[$floor TO ${today()}] AND (",
        separator = " OR ",
        postfix = ")",
        transform = { "arid:\"$it\"" },
    )

    /**
     * Attribute each release-group to the first `artist-credit` whose id is in
     * the user's favorites set, falling back to the first credit overall (so
     * a collaboration between a favorite and a non-favorite artist is
     * attributed to the favorite one). Releases with no artist credit at
     * all are skipped — they can't be denormalized safely.
     */
    private fun List<ReleaseGroupDto>.toNewReleaseEntities(
        favoriteIdsSet: Set<String>,
        now: Instant,
    ): List<NewReleaseEntity> = mapNotNull { dto ->
        val matchedArtist = dto.artistCredit
            .firstOrNull { it.artist.id in favoriteIdsSet }
            ?.artist
            ?: dto.artistCredit.firstOrNull()?.artist
            ?: return@mapNotNull null

        NewReleaseEntity(
            id = dto.id,
            artistId = matchedArtist.id,
            artistName = matchedArtist.name,
            title = dto.title,
            disambiguation = dto.disambiguation,
            firstReleaseDate = dto.firstReleaseDate?.toString(),
            primaryType = dto.primaryType ?: ReleaseType.OTHER,
            secondaryTypes = dto.secondaryTypes
                .joinToString(",")
                .takeIf(String::isNotEmpty),
            coverFrontUrl = null,
            state = NewReleaseState.UNSEEN,
            discoveredAt = now,
        )
    }

    override suspend fun markSeen(releaseId: String) {
        newReleasesDao.markSeen(releaseId)
    }

    override suspend fun markDismissed(releaseId: String) {
        newReleasesDao.markDismissed(releaseId)
    }

    override suspend fun markUnseen(releaseId: String) {
        newReleasesDao.markUnseen(releaseId)
    }

    override suspend fun restoreAllDismissed() {
        newReleasesDao.restoreAllDismissed()
    }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private fun nowInstant(): Instant = Clock.System.now()

    companion object {
        const val BATCH_SIZE = 50
    }
}
