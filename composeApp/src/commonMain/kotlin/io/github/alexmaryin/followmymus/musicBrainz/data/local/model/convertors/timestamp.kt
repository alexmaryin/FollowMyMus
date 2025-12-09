package io.github.alexmaryin.followmymus.musicBrainz.data.local.model.convertors

import androidx.room.TypeConverter
import kotlin.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromInstantToString(instant: Instant?): String? {
        return instant?.toString()
    }

    @TypeConverter
    fun fromStringToInstant(string: String?): Instant? {
        return string?.let { Instant.parse(it) }
    }
}
