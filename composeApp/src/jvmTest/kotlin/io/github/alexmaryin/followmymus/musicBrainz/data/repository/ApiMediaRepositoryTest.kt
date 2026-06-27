package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pagination tests for [ApiMediaRepository.fetchReleasesMedia].
 * A release can contain up to MAX_MEDIA_PAGES * API_PAGE media items
 * (each a CD / vinyl side), so the loop must walk all pages.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ApiMediaRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val originalMaxPages = PagingDefaults.MAX_MEDIA_PAGES

    @BeforeTest
    fun setUp() {
        PagingDefaults.MAX_MEDIA_PAGES = originalMaxPages
    }

    @AfterTest
    fun tearDown() {
        PagingDefaults.MAX_MEDIA_PAGES = originalMaxPages
    }

    private suspend fun kotlinx.coroutines.test.TestScope.runWithErrorDrain(
        repo: ApiMediaRepository,
        body: suspend kotlinx.coroutines.test.TestScope.() -> Unit
    ) {
        backgroundScope.launch { repo.errors.collect { /* drain */ } }
        body()
    }

    @Test
    fun `fetch fetches all pages when total exceeds one page`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val mediaDao = RecordingMediaDao()
        val repo = ApiMediaRepository(search, FakeCoversEngine(), mediaDao)
        runWithErrorDrain(repo) {
            // 124 media: page0 = 50, page1 = 50, page2 = 24
            search.enqueueMedia(
                mediaResponseWith(index = 50, count = 124, trackCount = 2),
                mediaResponseWith(index = 50, count = 124, trackCount = 2),
                mediaResponseWith(index = 24, count = 124, trackCount = 2),
            )
            repo.fetchReleasesMedia("rel-1")
            advanceUntilIdle()

            assertEquals(3, search.mediaCalls.size)
            assertEquals(listOf(0, 50, 100), search.mediaCalls.map { it.offset })
            assertEquals(50, search.mediaCalls.map { it.limit }.distinct().single())
            assertEquals(124, mediaDao.insertedMedia.size)
            // 124 media * 1 item * 2 tracks each = 248 tracks
            assertEquals(248, mediaDao.insertedTracks.size)
            assertEquals(WorkState.IDLE, repo.workState.value)
            assertEquals(124, repo.totalNetworkMedia.first())
        }
    }

    @Test
    fun `fetch reports PARTIAL_SYNC when a per-page error occurs after the first page`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val mediaDao = RecordingMediaDao()
        val repo = ApiMediaRepository(search, FakeCoversEngine(), mediaDao)
        runWithErrorDrain(repo) {
            search.enqueueMedia(
                mediaResponseWith(index = 50, count = 124),
                errorResult("midway 503"),
            )
            repo.fetchReleasesMedia("rel-1")
            advanceUntilIdle()

            assertEquals(2, search.mediaCalls.size)
            assertEquals(50, mediaDao.insertedMedia.size)
            assertEquals(WorkState.PARTIAL_SYNC, repo.workState.value)
            assertEquals(124, repo.totalNetworkMedia.first())
        }
    }

    @Test
    fun `fetch reports IDLE on first-page error because no work was done`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val mediaDao = RecordingMediaDao()
        val repo = ApiMediaRepository(search, FakeCoversEngine(), mediaDao)
        runWithErrorDrain(repo) {
            search.enqueueMedia(errorResult("initial 500"))
            repo.fetchReleasesMedia("rel-1")
            advanceUntilIdle()

            assertEquals(1, search.mediaCalls.size)
            assertEquals(0, mediaDao.insertedMedia.size)
            assertEquals(WorkState.IDLE, repo.workState.value)
            assertNull(repo.totalNetworkMedia.first())
        }
    }

    @Test
    fun `fetch respects MAX_MEDIA_PAGES and emits PARTIAL_SYNC on cap hit`() = runTest(dispatcher) {
        PagingDefaults.MAX_MEDIA_PAGES = 2
        val search = FakePagingSearchEngine()
        val mediaDao = RecordingMediaDao()
        val repo = ApiMediaRepository(search, FakeCoversEngine(), mediaDao)
        runWithErrorDrain(repo) {
            search.enqueueMedia(
                mediaResponseWith(index = 50, count = 10_000),
                mediaResponseWith(index = 50, count = 10_000),
            )
            repo.fetchReleasesMedia("rel-1")
            advanceUntilIdle()

            assertEquals(2, search.mediaCalls.size)
            assertEquals(100, mediaDao.insertedMedia.size)
            assertEquals(WorkState.PARTIAL_SYNC, repo.workState.value)
            assertEquals(10_000, repo.totalNetworkMedia.first())
        }
    }

    @Test
    fun `fetch is cooperatively cancellable between pages`() = runTest(dispatcher) {
        val search = FakePagingSearchEngine()
        val mediaDao = RecordingMediaDao()
        val repo = ApiMediaRepository(search, FakeCoversEngine(), mediaDao)
        backgroundScope.launch { repo.errors.collect { /* drain */ } }
        backgroundScope.launch { repo.totalNetworkMedia.collect { /* keep alive */ } }

        val gated = GatedPagingSearchEngine(base = search, pauseOnMediaCall = 2)
        val repoGated = ApiMediaRepository(gated, FakeCoversEngine(), mediaDao)

        search.enqueueMedia(
            mediaResponseWith(index = 50, count = 10_000),
            mediaResponseWith(index = 50, count = 10_000),
            mediaResponseWith(index = 50, count = 10_000),
        )

        val job = launch {
            assertFailsWith<CancellationException> { repoGated.fetchReleasesMedia("rel-1") }
        }
        advanceUntilIdle()
        // first media page processed, second page is now blocked on mediaGate
        job.cancel()
        advanceUntilIdle()
        gated.mediaGate.complete(kotlin.Unit)
        advanceUntilIdle()

        assertTrue(search.mediaCalls.size <= 2, "expected at most 2 search calls, got ${search.mediaCalls.size}")
        assertTrue(mediaDao.insertedMedia.size <= 100, "expected at most 100 inserted media, got ${mediaDao.insertedMedia.size}")
    }
}
