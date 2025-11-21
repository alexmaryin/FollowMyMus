package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.convertors

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
