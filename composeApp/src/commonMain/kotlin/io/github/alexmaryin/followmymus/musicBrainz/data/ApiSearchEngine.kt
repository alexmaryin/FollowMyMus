package io.github.alexmaryin.followmymus.musicBrainz.data

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.SearchResponse
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
    val httpClient: HttpClient
) : SearchEngine {

    override suspend fun searchArtists(query: String, offset: Int, limit: Int): SearchResponse {
        return withContext(Dispatchers.IO) {
            httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
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
        }
    }

    private fun String.surroundWithQuotation() = "\"$this\""
}