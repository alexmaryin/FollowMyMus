package io.github.alexmaryin.followmymus.musicBrainz.data

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.SearchResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.koin.core.annotation.Single

@Single(binds = [SearchEngine::class])
class ApiSearchEngine(
    val httpClient: HttpClient
) : SearchEngine {

    override suspend fun searchArtists(query: String, offset: Int, limit: Int): SearchResponse {
        return httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
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


//    override suspend fun searchArtists(query: String, offset: Int, limit: Int): Result<SearchResponse> = try {
//        val response: SearchResponse = httpClient.get("${SearchEngine.MB_BASE_URL}/artist/") {
//            url {
//                parameters.append("query", query.surroundWithQuotation())
//                parameters.append("fmt", "json")
//                parameters.append("offset", offset.toString())
//                parameters.append("limit", limit.toString())
//            }
//            headers {
//                append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
//            }
//        }.body()
//        Result.Success(response)
//    } catch (_: NoTransformationFoundException) {
//        Result.Error(SearchError.InvalidResponse)
//    } catch (e: ResponseException) {
//        Result.Error(SearchError.ServerError(e.response.status.value), e.message)
//    } catch (e: Exception) {
//        Result.Error(SearchError.NetworkError, e.message)
//    }
    
    private fun String.surroundWithQuotation() = "\"$this\""
}