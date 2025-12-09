package io.github.alexmaryin.followmymus.musicBrainz.data.local.db

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MusicBrainzDbConstructor : RoomDatabaseConstructor<MusicBrainzDatabase> {
    override fun initialize(): MusicBrainzDatabase
}