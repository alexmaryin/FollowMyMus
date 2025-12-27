package io.github.alexmaryin.followmymus.musicBrainz.data.remote

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.CoverArtResponse
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.koin.core.annotation.Single

@Single(binds = [CoversEngine::class])
class ApiCoversEngine(
    private val httpClient: HttpClient
) : CoversEngine {
    private suspend fun getCovers(url: String): Result<CoverArtResponse> = safeCall {
        val response: CoverArtResponse =
            httpClient.get(url) {
                url {
                    parameters.append("fmt", "json")
                }
                headers {
                    append("User-Agent", "FollowMyMus/1.0.0 (java.ul@gmail.com)")
                }
            }.body()
        response
    }

    override suspend fun getReleaseCovers(releaseId: String): Result<CoverArtResponse> =
        getCovers("${CoversEngine.COVER_ART_RELEASE_URL}/$releaseId")


    override suspend fun getMediaCovers(mediaId: String): Result<CoverArtResponse> =
        getCovers("${CoversEngine.COVER_ART_MEDIA_URL}/$mediaId")
}