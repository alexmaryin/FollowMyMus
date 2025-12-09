package io.github.alexmaryin.followmymus.musicBrainz.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.alexmaryin.followmymus.core.system.getAppDataDir
import java.io.File

actual class MusicBrainzDbFactory() {
    actual fun create(): RoomDatabase.Builder<MusicBrainzDatabase> {
        val appDataDir = getAppDataDir()
        if (!appDataDir.exists()) appDataDir.mkdirs()
        val dbFile = File(appDataDir, MusicBrainzDatabase.DB_NAME)
        return Room.databaseBuilder(dbFile.absolutePath)
    }
}