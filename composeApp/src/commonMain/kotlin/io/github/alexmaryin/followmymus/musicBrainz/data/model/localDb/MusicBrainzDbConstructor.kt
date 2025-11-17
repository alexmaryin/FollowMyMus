package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MusicBrainzDbConstructor : RoomDatabaseConstructor<MusicBrainzDatabase> {
    override fun initialize(): MusicBrainzDatabase
}