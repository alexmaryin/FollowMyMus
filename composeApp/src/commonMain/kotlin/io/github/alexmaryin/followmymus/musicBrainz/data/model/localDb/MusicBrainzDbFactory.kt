package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.RoomDatabase

expect class MusicBrainzDbFactory {
    fun create(): RoomDatabase.Builder<MusicBrainzDatabase>
}