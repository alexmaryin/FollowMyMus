package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums

/**
 * Thumbnail size keys from the Cover Art Archive API
 */
enum class ThumbnailSize(val jsonKey: String) {
    SIZE_250("250"),
    SIZE_500("500"),
    SIZE_1200("1200"),
    SMALL("small"),
    LARGE("large");

    companion object {
        fun fromJsonKey(key: String): ThumbnailSize? =
            entries.find { it.jsonKey == key }
    }
}