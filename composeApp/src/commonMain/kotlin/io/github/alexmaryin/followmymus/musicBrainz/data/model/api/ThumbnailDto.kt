package io.github.alexmaryin.followmymus.musicBrainz.data.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ThumbnailSize
import kotlinx.serialization.Serializable

/**
 * Individual thumbnail with size enum and URL
 */
@Serializable
data class ThumbnailDto(
    val size: ThumbnailSize,
    val url: String
)
