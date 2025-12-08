package io.github.alexmaryin.followmymus.musicBrainz.data

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.BrainzApiError
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.CoverArtResponse
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.SearchResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import org.koin.core.annotation.Single

@Single(binds = [SearchEngine::class])
class ApiSearchEngine(
    private val httpClient: HttpClient
) : SearchEngine {

    val artistCache = mutableMapOf<String, ArtistDto>()

    override suspend fun searchArtists(query: String, offset: Int, limit: Int): SearchResponse {
        return withContext(Dispatchers.IO) {
            val body: SearchResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
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

    override fun getArtistFromCache(artistId: String): ArtistDto? = artistCache[artistId].also {
        println("Successfully extract $it")
    }

    override suspend fun fetchArtistsById(ids: List<String>) = safeCall {
        val response: SearchResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
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

    override suspend fun searchCovers(releaseId: String): Result<CoverArtResponse> = safeCall {
        val response: CoverArtResponse =
            httpClient.get("${SearchEngine.COVER_ART_BASE_URL}/release-group/$releaseId") {
                url {
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }.body()
        response
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

    private fun String.surroundWithQuotation() = "\"$this\""

    private fun List<String>.toArtistIdsQuery() = joinToString(
        transform = { "arid:" + it.surroundWithQuotation() },
        separator = " OR "
    )

    private suspend fun <T> safeCall(call: suspend () -> T) = try {
        val result = withContext(Dispatchers.IO) { call() }
        Result.Success(result)
    } catch (_: HttpRequestTimeoutException) {
        Result.Error(BrainzApiError.Timeout)
    } catch (e: ServerResponseException) {
        Result.Error(BrainzApiError.ServerError(e.message))
    } catch (e: ClientRequestException) {
        Result.Error(BrainzApiError.NetworkError(e.message))
    } catch (_: SerializationException) {
        Result.Error(BrainzApiError.InvalidResponse)
    } catch (_: IllegalArgumentException) {
        Result.Error(BrainzApiError.MappingError)
    } catch (e: NoTransformationFoundException) {
        println(e.message)
        if (e.message.contains("coverart"))
            Result.Error(BrainzApiError.NoCoverError) else
            Result.Error(BrainzApiError.MappingError)
    } catch (e: Exception) {
        Result.Error(BrainzApiError.Unknown(e.message))
    }
}