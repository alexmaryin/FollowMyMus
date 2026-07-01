package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FavoritesImportExportRepositoryTest {

    private val favoriteDao = mockk<FavoriteDao>()
    private val artistsRepository = mockk<ArtistsRepository>()
    private val searchEngine = mockk<SearchEngine>()

    private lateinit var repository: FavoritesImportExportRepository

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @BeforeTest
    fun setUp() {
        repository = FavoritesImportExportRepository(favoriteDao, searchEngine, artistsRepository)
    }

    private fun makeDto(id: String) = ArtistDto(
        id = id, type = null, score = null, name = "Artist $id", sortName = "",
        area = null, beginArea = null, disambiguation = null, lifeSpan = null,
        releaseCount = 0, releases = emptyList()
    )

    // ─── serializeExport ─────────────────────────────────────────────────

    @Test
    fun `serializeExport with N favorites returns Success with lex-sorted MBIDs`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(listOf("z-mbid", "a-mbid", "m-mbid"))

        val result = repository.serializeExport()
        val payload = assertIs<Result.Success<FavoritesExportPayload>>(result)
        assertEquals(3, payload.value.count)

        val decoded = json.decodeFromString<FavoritesExportFile>(payload.value.bytes.decodeToString())
        assertEquals("followmymus.favorites", decoded.format)
        assertEquals(1, decoded.version)
        assertEquals(listOf("a-mbid", "m-mbid", "z-mbid"), decoded.artists)
    }

    @Test
    fun `serializeExport with 0 favorites returns Success with empty artists array`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())

        val result = repository.serializeExport()
        val payload = assertIs<Result.Success<FavoritesExportPayload>>(result)
        assertEquals(0, payload.value.count)

        val decoded = json.decodeFromString<FavoritesExportFile>(payload.value.bytes.decodeToString())
        assertEquals(emptyList<String>(), decoded.artists)
        assertEquals(1, decoded.version)
    }

    @Test
    fun `serializeExport when Room throws returns Error DataReadError`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } throws RuntimeException("DB is locked")

        val result = repository.serializeExport()
        val err = assertIs<Result.Error>(result)
        assertIs<FavoritesImportExportError.DataReadError>(err.type)
    }

    // ─── importFromBytes: validation ─────────────────────────────────────

    @Test
    fun `importFromBytes with valid file and 5 new MBIDs imports all 5`() = runTest {
        val ids = (1..5).map { "mbid-$it" }
        val bytes = makeValidV1File(ids)

        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())
        coEvery { searchEngine.searchArtistsByIdBatch(any()) } returns ids.map { makeDto(it) }
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(5, summary.value.imported)
        assertEquals(0, summary.value.skipped)
        assertEquals(0, summary.value.failed)

        coVerify(exactly = 5) { artistsRepository.addToFavorite(any()) }
    }

    @Test
    fun `importFromBytes with 3 already-favorite MBIDs skips all, no MusicBrainz fetch`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(listOf("mbid-1", "mbid-2", "mbid-3"))
        val bytes = makeValidV1File(listOf("mbid-1", "mbid-2", "mbid-3"))

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(0, summary.value.imported)
        assertEquals(3, summary.value.skipped)
        assertEquals(0, summary.value.failed)

        coVerify(exactly = 0) { searchEngine.searchArtistsByIdBatch(any()) }
        coVerify(exactly = 0) { artistsRepository.addToFavorite(any()) }
    }

    @Test
    fun `importFromBytes with mixed already and new returns correct counts`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(listOf("mbid-1", "mbid-2"))
        val bytes = makeValidV1File(listOf("mbid-1", "mbid-2", "mbid-3", "mbid-4", "mbid-5"))

        coEvery { searchEngine.searchArtistsByIdBatch(any()) } returns listOf("mbid-3", "mbid-4", "mbid-5").map { makeDto(it) }
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(3, summary.value.imported)
        assertEquals(2, summary.value.skipped)
        assertEquals(0, summary.value.failed)

        coVerify(exactly = 3) { artistsRepository.addToFavorite(any()) }
    }

    @Test
    fun `importFromBytes with 5 new MBIDs where 2 are deleted returns partial success`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())
        val bytes = makeValidV1File(listOf("mbid-1", "mbid-2", "mbid-3", "mbid-4", "mbid-5"))

        val returnedDtos = listOf("mbid-1", "mbid-3", "mbid-5").map { makeDto(it) }
        coEvery { searchEngine.searchArtistsByIdBatch(any()) } returns returnedDtos
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(3, summary.value.imported)
        assertEquals(0, summary.value.skipped)
        assertEquals(2, summary.value.failed)
    }

    @Test
    fun `importFromBytes with all new MBIDs failing returns NetworkError`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())
        val bytes = makeValidV1File(listOf("mbid-1", "mbid-2"))

        coEvery { searchEngine.searchArtistsByIdBatch(any()) } returns emptyList()
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        assertIs<FavoritesImportExportError.NetworkError>(err.type)
    }

    @Test
    fun `importFromBytes with version 2 returns UnsupportedVersion`() = runTest {
        val file = FavoritesExportFile(
            format = "followmymus.favorites",
            version = 2,
            exportedAt = "2026-07-01T12:00:00Z",
            artists = listOf("mbid-1")
        )
        val bytes = json.encodeToString(file).encodeToByteArray()

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        val verErr = assertIs<FavoritesImportExportError.UnsupportedVersion>(err.type)
        assertEquals(2, verErr.version)
    }

    @Test
    fun `importFromBytes with wrong format returns UnsupportedFormat`() = runTest {
        val file = FavoritesExportFile(
            format = "other.app.favorites",
            version = 1,
            exportedAt = "2026-07-01T12:00:00Z",
            artists = listOf("mbid-1")
        )
        val bytes = json.encodeToString(file).encodeToByteArray()

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        assertIs<FavoritesImportExportError.UnsupportedFormat>(err.type)
    }

    @Test
    fun `importFromBytes with non-JSON bytes returns Malformed`() = runTest {
        val bytes = "this is not json".encodeToByteArray()

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        assertIs<FavoritesImportExportError.Malformed>(err.type)
    }

    @Test
    fun `importFromBytes with missing artists field returns MissingArtistsField`() = runTest {
        val text = """{"format":"followmymus.favorites","version":1,"exportedAt":"2026-07-01T12:00:00Z"}"""
        val bytes = text.encodeToByteArray()

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        assertIs<FavoritesImportExportError.MissingArtistsField>(err.type)
    }

    @Test
    fun `importFromBytes with empty string in artists returns EmptyArtistEntry`() = runTest {
        val file = FavoritesExportFile(
            format = "followmymus.favorites",
            version = 1,
            exportedAt = "2026-07-01T12:00:00Z",
            artists = listOf("mbid-1", "", "mbid-3")
        )
        val bytes = json.encodeToString(file).encodeToByteArray()

        val result = repository.importFromBytes(bytes)
        val err = assertIs<Result.Error>(result)
        val entryErr = assertIs<FavoritesImportExportError.EmptyArtistEntry>(err.type)
        assertEquals(1, entryErr.index)
    }

    @Test
    fun `importFromBytes with duplicate MBIDs deduplicates correctly`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())
        val bytes = makeValidV1File(listOf("mbid-1", "mbid-1", "mbid-1", "mbid-2"))

        coEvery { searchEngine.searchArtistsByIdBatch(any()) } answers {
            val ids = firstArg<List<String>>()
            ids.map { makeDto(it) }
        }
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(2, summary.value.imported + summary.value.skipped + summary.value.failed)
    }

    @Test
    fun `importFromBytes with 250 new MBIDs chunks into 3 batches`() = runTest {
        every { favoriteDao.getFavoriteArtistsIds() } returns flowOf(emptyList())
        val allIds = (1..250).map { "mbid-$it" }
        val bytes = makeValidV1File(allIds)

        val batchSizes = mutableListOf<Int>()
        coEvery { searchEngine.searchArtistsByIdBatch(any()) } answers {
            val ids = firstArg<List<String>>()
            batchSizes += ids.size
            ids.map { makeDto(it) }
        }
        coEvery { artistsRepository.addToFavorite(any()) } just runs

        val result = repository.importFromBytes(bytes)
        val summary = assertIs<Result.Success<ImportSummary>>(result)
        assertEquals(250, summary.value.imported)
        assertEquals(listOf(100, 100, 50), batchSizes)
    }

    @Test
    fun `importFromBytes sourceName appears in Malformed error message`() = runTest {
        val bytes = "garbage".encodeToByteArray()

        val result = repository.importFromBytes(bytes, sourceName = "/path/to/file.json")
        val err = assertIs<Result.Error>(result)
        assertTrue(err.message?.contains("/path/to/file.json") == true)
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private fun makeValidV1File(ids: List<String>): ByteArray {
        val file = FavoritesExportFile(
            format = "followmymus.favorites",
            version = 1,
            exportedAt = "2026-07-01T12:00:00Z",
            artists = ids
        )
        return json.encodeToString(file).encodeToByteArray()
    }
}
