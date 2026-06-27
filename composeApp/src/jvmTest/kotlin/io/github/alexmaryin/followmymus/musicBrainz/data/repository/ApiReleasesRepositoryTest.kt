package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pagination tests for [ApiReleasesRepository.syncReleases].
 * The full ArtistDto for any artist is at most MAX_RELEASE_PAGES * API_PAGE
 * items; this covers the happy path, a mid-sync error, a cap hit and
 * cooperative cancellation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ApiReleasesRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val originalMaxPages = PagingDefaults.MAX_RELEASE_PAGES

    @BeforeTest
    fun setUp() {
        PagingDefaults.MAX_RELEASE_PAGES = originalMaxPages
    }

    @AfterTest
    fun tearDown() {
        PagingDefaults.MAX_RELEASE_PAGES = originalMaxPages
    }

    private fun newRepo(
        search: FakePagingSearchEngine = FakePagingSearchEngine(),
        covers: FakeCoversEngine = FakeCoversEngine(),
        releasesDao: RecordingReleasesDao = RecordingReleasesDao(),
        resourceDao: RecordingResourceDao = RecordingResourceDao(),
        transactionalDao: RecordingTransactionalDao = RecordingTransactionalDao()
    ): ApiReleasesRepository =
        ApiReleasesRepository(search, covers, releasesDao, resourceDao, transactionalDao)

    /**
     * Runs [body] with a background job draining [repo.errors] so that
     * `Channel.send` from the repository never blocks. Without this,
     * any `forError` branch would suspend forever in tests.
     */
    private suspend fun kotlinx.coroutines.test.TestScope.runWithErrorDrain(
        repo: ApiReleasesRepository,
        body: suspend kotlinx.coroutines.test.TestScope.() -> Unit
    ) {
        backgroundScope.launch { repo.errors.collect { /* drain */ } }
        body()
    }

    @Test
    fun `sync fetches all pages when total exceeds one page`() = runTest(dispatcher) {
        val repo = newRepo()
        val search = FakePagingSearchEngine().also { repo ->
            // rewire repo to use this specific search engine
        }

        // Rebuild repo with a dedicated search engine
        val releaseDao = RecordingReleasesDao()
        val repo2 = ApiReleasesRepository(
            search, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )
        runWithErrorDrain(repo2) {
            search.enqueueReleases(
                successReleases(index = 50, count = 137),
                successReleases(index = 50, count = 137),
                successReleases(index = 37, count = 137),
            )
            repo2.syncReleases("artist-1")
            advanceUntilIdle()

            assertEquals(3, search.releasesCalls.size)
            assertEquals(listOf(0, 50, 100), search.releasesCalls.map { it.offset })
            assertEquals(50, search.releasesCalls.map { it.limit }.distinct().single())
            assertEquals(137, releaseDao.insertedReleases.size)
            assertEquals(WorkState.IDLE, repo2.workState.value)
            assertEquals(137, repo2.totalNetworkReleases.first())
        }
    }

    @Test
    fun `sync reports PARTIAL_SYNC when a per-page error occurs after the first page`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val releaseDao = RecordingReleasesDao()
        val repo = ApiReleasesRepository(
            search, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )
        runWithErrorDrain(repo) {
            search.enqueueReleases(
                successReleases(index = 50, count = 137),
                errorResult("midway 503"),
            )
            repo.syncReleases("artist-1")
            advanceUntilIdle()

            assertEquals(2, search.releasesCalls.size)
            assertEquals(50, releaseDao.insertedReleases.size)
            assertEquals(WorkState.PARTIAL_SYNC, repo.workState.value)
            assertEquals(137, repo.totalNetworkReleases.first())
        }
    }

    @Test
    fun `sync reports IDLE on first-page error because no work was done`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val releaseDao = RecordingReleasesDao()
        val repo = ApiReleasesRepository(
            search, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )
        runWithErrorDrain(repo) {
            search.enqueueReleases(errorResult("initial 500"))
            repo.syncReleases("artist-1")
            advanceUntilIdle()

            assertEquals(1, search.releasesCalls.size)
            assertEquals(0, releaseDao.insertedReleases.size)
            assertEquals(WorkState.IDLE, repo.workState.value)
            assertNull(repo.totalNetworkReleases.first())
        }
    }

    @Test
    fun `sync respects MAX_RELEASE_PAGES and emits PARTIAL_SYNC on cap hit`() = runTest(dispatcher) {
        PagingDefaults.MAX_RELEASE_PAGES = 2
        val search = FakePagingSearchEngine()
        val releaseDao = RecordingReleasesDao()
        val repo = ApiReleasesRepository(
            search, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )
        runWithErrorDrain(repo) {
            search.enqueueReleases(
                successReleases(index = 50, count = 10_000),
                successReleases(index = 50, count = 10_000),
            )
            repo.syncReleases("artist-1")
            advanceUntilIdle()

            assertEquals(2, search.releasesCalls.size)
            assertEquals(100, releaseDao.insertedReleases.size)
            assertEquals(WorkState.PARTIAL_SYNC, repo.workState.value)
            assertEquals(10_000, repo.totalNetworkReleases.first())
        }
    }

    @Test
    fun `sync is cooperatively cancellable between pages`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val releaseDao = RecordingReleasesDao()
        val repo = ApiReleasesRepository(
            search, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )
        backgroundScope.launch { repo.errors.collect { /* drain */ } }
        backgroundScope.launch { repo.totalNetworkReleases.collect { /* keep alive */ } }

        // The gated engine suspends on the second release call (offset = 50).
        val gated = GatedPagingSearchEngine(base = search, pauseOnReleaseCall = 2)
        val repoGated = ApiReleasesRepository(
            gated, FakeCoversEngine(), releaseDao,
            RecordingResourceDao(), RecordingTransactionalDao()
        )

        search.enqueueReleases(
            successReleases(index = 50, count = 10_000),
            successReleases(index = 50, count = 10_000),
            successReleases(index = 50, count = 10_000),
        )

        val job = launch {
            assertFailsWith<CancellationException> { repoGated.syncReleases("artist-1") }
        }
        advanceUntilIdle()
        // first page processed, second page is now blocked on releaseGate
        job.cancel()
        advanceUntilIdle()
        // release any still-blocked coroutines so the test scope can drain
        gated.releaseGate.complete(kotlin.Unit)
        advanceUntilIdle()

        assertTrue(search.releasesCalls.size <= 2, "expected at most 2 search calls, got ${search.releasesCalls.size}")
        assertTrue(releaseDao.insertedReleases.size <= 100, "expected at most 100 inserted releases, got ${releaseDao.insertedReleases.size}")
    }
}
