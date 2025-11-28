package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root response wrapper from Cover Art Archive API
 */
@Serializable
data class CoverArtResponse(
    @SerialName("images")
    val images: List<CoverImageDto>,
    @SerialName("release")
    val release: String
)
