package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ThumbnailSize
import kotlinx.serialization.Serializable

/**
 * Individual thumbnail with size enum and URL
 */
@Serializable
data class ThumbnailDto(
    val size: ThumbnailSize,
    val url: String
)
