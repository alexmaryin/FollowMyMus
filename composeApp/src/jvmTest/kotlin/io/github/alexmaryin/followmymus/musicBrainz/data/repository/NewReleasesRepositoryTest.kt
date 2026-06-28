package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.core.network.RateLimitedApiQueue
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseState
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.BrainzApiError
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.preferences.PreferenceSource
import io.github.alexmaryin.followmymus.preferences.getAppSettings
import io.github.alexmaryin.followmymus.preferences.setNewReleasesFloor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.*
import kotlin.time.Clock

class NewReleasesRepositoryTest {

    private lateinit var tempFile: File
    private lateinit var source: PreferenceSource
    private lateinit var favoriteDao: FakeFavoriteDao
    private lateinit var search: FakePagingSearchEngine
    private lateinit var newReleasesDao: FakeNewReleasesDao
    private val queue = RateLimitedApiQueue(intervalMs = 1L)

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("new_releases_test", ".preferences_pb").apply { deleteOnExit() }
        val prefs = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.absolutePath.toPath() }
        )
        source = PreferenceSource(prefs)
        favoriteDao = FakeFavoriteDao()
        search = FakePagingSearchEngine()
        newReleasesDao = FakeNewReleasesDao()
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    private fun newRepo(): ApiNewReleasesRepository = ApiNewReleasesRepository(
        searchEngine = search,
        rateLimitedApiQueue = queue,
        newReleasesDao = newReleasesDao,
        favoriteDao = favoriteDao,
        preferenceSource = source,
    )

    private suspend fun awaitIdle(repo: ApiNewReleasesRepository) {
        repo.workState.first { it == WorkState.IDLE || it == WorkState.PARTIAL_SYNC }
    }

    @Test
    fun `7_2_a 30 favorites and a single-page response yields one API call, 30 UNSEEN rows, IDLE, settings updated`() = runTest {
        favoriteDao.setIds((1..30).map { "artist-$it" })
        search.enqueueReleaseGroups(successReleaseGroups(ids = (1..30).map { "rg-$it" }))

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        assertEquals(1, search.releaseGroupsCalls.size)
        assertEquals(30, newReleasesDao.upserted.size)
        assertTrue(newReleasesDao.upserted.all { it.state == NewReleaseState.UNSEEN })
        assertEquals(WorkState.IDLE, repo.workState.value)
        val settings = source.getAppSettings().first()
        assertEquals(Clock.System.todayIn(TimeZone.currentSystemDefault()), settings.newReleasesLastOpenedDay)
    }

    @Test
    fun `7_2_b 500 favorites issue 10 API calls in 10 batches of 50`() = runTest {
        favoriteDao.setIds((1..500).map { "artist-$it" })
        repeat(10) { i ->
            search.enqueueReleaseGroups(
                successReleaseGroups(ids = (1..50).map { "rg-${i * 50 + it}" }),
            )
        }

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        assertEquals(10, search.releaseGroupsCalls.size)
        assertEquals(500, newReleasesDao.upserted.size)
        assertEquals(WorkState.IDLE, repo.workState.value)
    }

    @Test
    fun `7_2_c 30 favorites with a 250-result response paginates within the batch`() = runTest {
        favoriteDao.setIds(listOf("artist-1"))
        // The repo paginates at PagingDefaults.API_PAGE (50) per call, so a
        // 250-result response takes 5 pages: offsets 0, 50, 100, 150, 200.
        repeat(4) { i ->
            search.enqueueReleaseGroups(
                successReleaseGroups(ids = (1..50).map { "rg-${i * 50 + it}" }, count = 250),
            )
        }
        search.enqueueReleaseGroups(
            successReleaseGroups(ids = (1..50).map { "rg-4-${it}" }, count = 250),
        )

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        assertEquals(5, search.releaseGroupsCalls.size)
        assertEquals(listOf(0, 50, 100, 150, 200), search.releaseGroupsCalls.map { it.offset })
        assertEquals(250, newReleasesDao.upserted.size)
    }

    @Test
    fun `7_2_d pre-existing SEEN row keeps its state on a re-fetch`() = runTest {
        // Pre-load the dao with a SEEN row that the sync will re-upsert.
        newReleasesDao.upserted += newReleaseEntity("rg-1", state = NewReleaseState.SEEN)
        favoriteDao.setIds(listOf("artist-1"))
        search.enqueueReleaseGroups(
            successReleaseGroups(ids = listOf("rg-1", "rg-2")),
        )

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        // The original SEEN entry is still the first row, with state=SEEN.
        val seen = newReleasesDao.upserted.first { it.id == "rg-1" }
        assertEquals(NewReleaseState.SEEN, seen.state)
    }

    @Test
    fun `7_2_e one batch error yields PARTIAL_SYNC and the floor is NOT updated`() = runTest {
        favoriteDao.setIds((1..100).map { "artist-$it" })
        search.enqueueReleaseGroups(
            successReleaseGroups(ids = (1..50).map { "rg-1-$it" }),
            // Second batch fails.
            Result.Error(BrainzApiError.NetworkError("503")),
        )

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        assertEquals(WorkState.PARTIAL_SYNC, repo.workState.value)
        val settings = source.getAppSettings().first()
        assertNull(settings.newReleasesLastOpenedDay)
        assertNull(settings.newReleasesLastSyncCompletedAt)
    }

    @Test
    fun `7_2_g zero favorites makes no API calls, no DB writes, and settings stay empty`() = runTest {
        favoriteDao.setIds(emptyList())
        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        assertEquals(0, search.releaseGroupsCalls.size)
        assertEquals(0, newReleasesDao.upserted.size)
        assertEquals(WorkState.IDLE, repo.workState.value)
        assertNull(source.getAppSettings().first().newReleasesLastOpenedDay)
    }

    @Test
    fun `7_2_h first sync ever uses today minus 30 days as the floor`() = runTest {
        favoriteDao.setIds(listOf("artist-1"))
        search.enqueueReleaseGroups(successReleaseGroups(ids = listOf("rg-1")))

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val expectedFloor = today.minus(30, DateTimeUnit.DAY)
        val query = search.releaseGroupsCalls.single().query
        val dateMatch = Regex("""firstreleasedate:\[(\d{4}-\d{2}-\d{2}) TO \d{4}-\d{2}-\d{2}]""").find(query)
        assertTrue(dateMatch != null, "expected `firstreleasedate:[YYYY-MM-DD TO YYYY-MM-DD]` in query, got: $query")
        val floor = LocalDate.parse(dateMatch.groupValues[1])
        assertEquals(expectedFloor, floor)
    }

    @Test
    fun `7_2_i subsequent sync uses previous floor plus one day as the new floor`() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val previousFloor = today.minus(7, DateTimeUnit.DAY)
        source.setNewReleasesFloor(previousFloor, Clock.System.now())

        favoriteDao.setIds(listOf("artist-1"))
        search.enqueueReleaseGroups(successReleaseGroups(ids = listOf("rg-1")))

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        val expectedNewFloor = previousFloor.plus(1, DateTimeUnit.DAY)
        val query = search.releaseGroupsCalls.single().query
        val dateMatch = Regex("""firstreleasedate:\[(\d{4}-\d{2}-\d{2}) TO \d{4}-\d{2}-\d{2}]""").find(query)
        assertTrue(dateMatch != null)
        val floor = LocalDate.parse(dateMatch.groupValues[1])
        assertEquals(expectedNewFloor, floor)
    }

    @Test
    fun `7_2_j same-day re-open clamps the floor to today so the range is never inverted`() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        source.setNewReleasesFloor(today, Clock.System.now())

        favoriteDao.setIds(listOf("artist-1"))
        search.enqueueReleaseGroups(successReleaseGroups(ids = listOf("rg-1")))

        val repo = newRepo()
        repo.syncNewReleases()
        awaitIdle(repo)

        val query = search.releaseGroupsCalls.single().query
        val dateMatch = Regex("""firstreleasedate:\[(\d{4}-\d{2}-\d{2}) TO (\d{4}-\d{2}-\d{2})]""").find(query)
        assertTrue(dateMatch != null, "expected `firstreleasedate:[YYYY-MM-DD TO YYYY-MM-DD]` in query, got: $query")
        val start = LocalDate.parse(dateMatch.groupValues[1])
        val end = LocalDate.parse(dateMatch.groupValues[2])
        assertTrue(start <= end, "range must never invert, got [$start TO $end]")
        assertEquals(today, start, "floor must be clamped to today on same-day re-open")
        assertEquals(today, end, "end must be today")
    }
}
