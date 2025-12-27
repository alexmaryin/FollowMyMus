package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.CoverArtResponse

interface CoversEngine {
    suspend fun getReleaseCovers(releaseId: String): Result<CoverArtResponse>

    suspend fun getMediaCovers(mediaId: String): Result<CoverArtResponse>

    companion object {
        const val COVER_ART_RELEASE_URL = "https://coverartarchive.org/release-group"
        const val COVER_ART_MEDIA_URL = "https://coverartarchive.org/release"
    }
}