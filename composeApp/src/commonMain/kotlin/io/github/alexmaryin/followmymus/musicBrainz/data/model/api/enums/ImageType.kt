package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import kotlinx.serialization.Serializable

/**
 * Type of cover art image determined from API flags
 */
@Serializable
enum class ImageType {
    FRONT, BACK, OTHER
}