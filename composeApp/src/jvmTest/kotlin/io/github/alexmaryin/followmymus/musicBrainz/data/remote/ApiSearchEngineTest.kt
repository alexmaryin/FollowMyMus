package io.github.alexmaryin.followmymus.musicBrainz.data.remote

import io.github.alexmaryin.followmymus.core.network.RateLimitedApiQueue
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchArtistResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApiSearchEngineTest {

    private val httpClient = mockk<HttpClient>()
    private val queue = RateLimitedApiQueue(intervalMs = 1L)
    private val engine = ApiSearchEngine(httpClient, queue)

    @AfterTest
    fun tearDown() {
        queue.cancel()
    }

    @Test
    fun `getArtistById returns Artist on 200`() = runTest {
        val dto = ArtistDto(
            id = "mbid-123",
            type = null,
            score = null,
            name = "Test Artist",
            sortName = "Test Artist",
            area = null,
            beginArea = null,
            disambiguation = null,
            lifeSpan = null,
            releaseCount = 0,
            releases = emptyList()
        )

        val mockResponse = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
        }
        coEvery { mockResponse.body<ArtistDto>() } returns dto

        coEvery {
            httpClient.get("${SearchEngine.MB_BASE_URL}/artist/mbid-123/") {
                url {
                    parameters.append("inc", "url-rels+release-groups")
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }
        } returns mockResponse

        val result = engine.getArtistById("mbid-123")
        assertNotNull(result)
        assertEquals("mbid-123", result.id)
        assertEquals("Test Artist", result.name)
    }

    @Test
    fun `getArtistById returns null on 404`() = runTest {
        val errorResponse = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.NotFound
        }

        coEvery {
            httpClient.get("${SearchEngine.MB_BASE_URL}/artist/unknown-id/") {
                url {
                    parameters.append("inc", "url-rels+release-groups")
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }
        } throws io.ktor.client.plugins.ClientRequestException(errorResponse, "")

        val result = engine.getArtistById("unknown-id")
        assertNull(result)
    }

    @Test
    fun `getArtistById returns null on network error`() = runTest {
        coEvery {
            httpClient.get("${SearchEngine.MB_BASE_URL}/artist/no-conn/") {
                url {
                    parameters.append("inc", "url-rels+release-groups")
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }
        } throws java.io.IOException("No network")

        val result = engine.getArtistById("no-conn")
        assertNull(result)
    }

    @Test
    fun `searchArtistsByIdBatch returns dtos and caches them`() = runTest {
        val mbid1 = "mbid-batch-1"
        val mbid2 = "mbid-batch-2"

        val mockResponse = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
        }
        coEvery { mockResponse.body<SearchArtistResponse>() } returns SearchArtistResponse(
            created = "2026-07-01T12:00:00Z",
            count = 2,
            offset = 0,
            artists = listOf(
                ArtistDto(id = mbid1, type = null, score = null, name = "Batch 1", sortName = "", area = null, beginArea = null, disambiguation = null, lifeSpan = null, releaseCount = 0, releases = emptyList()),
                ArtistDto(id = mbid2, type = null, score = null, name = "Batch 2", sortName = "", area = null, beginArea = null, disambiguation = null, lifeSpan = null, releaseCount = 0, releases = emptyList())
            )
        )

        coEvery {
            httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
                url {
                    parameters.append("query", """arid:"mbid-batch-1" OR arid:"mbid-batch-2"""")
                    parameters.append("limit", "100")
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }
        } returns mockResponse

        val result = engine.searchArtistsByIdBatch(listOf(mbid2, mbid1))
        assertEquals(2, result.size)
        assertEquals(mbid1, result[0].id)
        assertEquals(mbid2, result[1].id)

        val cached = engine.getArtistFromCache(mbid1)
        assertNotNull(cached)
    }
}
