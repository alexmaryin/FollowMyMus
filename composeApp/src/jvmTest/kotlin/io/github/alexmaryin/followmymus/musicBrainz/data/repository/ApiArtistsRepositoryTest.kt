package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiArtistsRepositoryTest {

    private val artistDao = mockk<ArtistDao>()
    private val favoriteDao = mockk<FavoriteDao>()
    private val supabaseDb = mockk<SupabaseDb>()
    private val searchEngine = mockk<SearchEngine>()
    private val dbRepository = mockk<LocalDbRepository>()

    private val repository = ApiArtistsRepository(artistDao, favoriteDao, supabaseDb, searchEngine, dbRepository)

    @Test
    fun `addToFavorite with cached artist uses cache and inserts`() = runTest {
        val dto = ArtistDto(id = "mbid-1", type = null, score = null, name = "Artist", sortName = "", area = null, beginArea = null, disambiguation = null, lifeSpan = null, releaseCount = 0, releases = emptyList())
        every { searchEngine.getArtistFromCache("mbid-1") } returns dto
        coEvery { searchEngine.getArtistById(any()) } returns null

        coEvery { dbRepository.insertArtist(any(), any()) } just runs
        coEvery { supabaseDb.addRemoteFavoriteArtist(any()) } returns Result.Success(Unit)
        coEvery { artistDao.updateSyncStatus(any(), any()) } just runs

        repository.addToFavorite("mbid-1")

        coVerify(exactly = 1) { dbRepository.insertArtist(any(), any()) }
        coVerify(exactly = 1) { supabaseDb.addRemoteFavoriteArtist(any()) }
        coVerify(exactly = 0) { searchEngine.getArtistById(any()) }
    }

    @Test
    fun `addToFavorite with uncached artist uses network fallback`() = runTest {
        val dto = ArtistDto(id = "mbid-2", type = null, score = null, name = "Artist 2", sortName = "", area = null, beginArea = null, disambiguation = null, lifeSpan = null, releaseCount = 0, releases = emptyList())
        every { searchEngine.getArtistFromCache("mbid-2") } returns null
        coEvery { searchEngine.getArtistById("mbid-2") } returns Artist(
            id = "mbid-2", name = "Artist 2", details = "", dtoSource = dto
        )

        coEvery { dbRepository.insertArtist(any(), any()) } just runs
        coEvery { supabaseDb.addRemoteFavoriteArtist(any()) } returns Result.Success(Unit)
        coEvery { artistDao.updateSyncStatus(any(), any()) } just runs

        repository.addToFavorite("mbid-2")

        coVerify(exactly = 1) { searchEngine.getArtistById("mbid-2") }
        coVerify(exactly = 1) { dbRepository.insertArtist(any(), any()) }
        coVerify(exactly = 1) { supabaseDb.addRemoteFavoriteArtist(any()) }
    }

    @Test
    fun `addToFavorite with neither cached nor network is no-op`() = runTest {
        every { searchEngine.getArtistFromCache("mbid-3") } returns null
        coEvery { searchEngine.getArtistById("mbid-3") } returns null

        repository.addToFavorite("mbid-3")

        coVerify(exactly = 0) { dbRepository.insertArtist(any(), any()) }
        coVerify(exactly = 0) { supabaseDb.addRemoteFavoriteArtist(any()) }
    }
}
