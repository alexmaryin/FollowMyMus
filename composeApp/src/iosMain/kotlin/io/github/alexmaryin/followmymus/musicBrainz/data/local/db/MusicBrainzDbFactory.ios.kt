package io.github.alexmaryin.followmymus.musicBrainz.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class MusicBrainzDbFactory {
    actual fun create(): RoomDatabase.Builder<MusicBrainzDatabase> {
        val dbPath = documentsPath() + MusicBrainzDatabase.DB_NAME
        return Room.databaseBuilder(name = dbPath)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): String {
    val documentDirectory = NSFileManager.Companion.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}