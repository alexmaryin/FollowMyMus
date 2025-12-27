package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.ImageSerializer
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ImageType
import kotlinx.serialization.Serializable

/**
 * Main image data class with custom serialization.
 * Deserializes from full API format, serializes to simplified format.
 */
@Serializable(with = ImageSerializer::class)
data class CoverImageDto(
    val type: ImageType,
    val url: String,
    val thumbnails: List<ThumbnailDto>
)