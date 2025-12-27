package io.github.alexmaryin.followmymus.musicBrainz.data.remote

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.MediaDto
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchArtistResponse
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.SearchMediaResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [SearchEngine::class])
class ApiSearchEngine(
    private val httpClient: HttpClient
) : SearchEngine {

    val artistCache = mutableMapOf<String, ArtistDto>()

    override suspend fun searchArtists(query: String, offset: Int, limit: Int): SearchArtistResponse {
        return withContext(Dispatchers.IO) {
            val body: SearchArtistResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
                url {
                    parameters.append("query", query.surroundWithQuotation())
                    parameters.append("fmt", "json")
                    parameters.append("offset", offset.toString())
                    parameters.append("limit", limit.toString())
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }.body()
            if (artistCache.size > SearchEngine.LIMIT * 2) artistCache.clear()
            artistCache.putAll(body.artists.associateBy { it.id })
            body
        }
    }

    override fun getArtistFromCache(artistId: String): ArtistDto? = artistCache[artistId]

    override suspend fun fetchArtistsById(ids: List<String>) = safeCall {
        val response: SearchArtistResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
            url {
                parameters.append("query", ids.toArtistIdsQuery())
                parameters.append("fmt", "json")
            }
            headers {
                append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
            }
        }.body()
        response.artists
    }

    override suspend fun searchReleases(artistId: String) = safeCall {
        val response: ArtistDto = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/$artistId/") {
            url {
                parameters.append("inc", "url-rels+release-groups")
                parameters.append("fmt", "json")
            }
            headers {
                append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
            }
        }.body()
        response
    }

    override suspend fun searchMedia(releaseId: String): Result<List<MediaDto>> = safeCall {
        val response: SearchMediaResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/release/") {
            url {
                parameters.append("release-group", releaseId)
                parameters.append("inc", "media+recordings+url-rels")
                parameters.append("fmt", "json")
            }
            headers {
                append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
            }
        }.body()
        response.releases
    }

    private fun String.surroundWithQuotation() = "\"$this\""

    private fun List<String>.toArtistIdsQuery() = joinToString(
        transform = { "arid:" + it.surroundWithQuotation() },
        separator = " OR "
    )
}