package io.github.alexmaryin.followmymus.musicBrainz.data.local.db

import androidx.room.RoomDatabase

expect class MusicBrainzDbFactory {
    fun create(): RoomDatabase.Builder<MusicBrainzDatabase>
}