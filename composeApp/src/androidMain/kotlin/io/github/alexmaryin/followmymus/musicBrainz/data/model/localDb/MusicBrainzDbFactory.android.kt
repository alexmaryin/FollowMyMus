package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class MusicBrainzDbFactory() : KoinComponent {

    private val context: Context by inject()
    actual fun create(): RoomDatabase.Builder<MusicBrainzDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(MusicBrainzDatabase.DB_NAME)
        return Room.databaseBuilder(appContext, dbFile.absolutePath)
    }
}