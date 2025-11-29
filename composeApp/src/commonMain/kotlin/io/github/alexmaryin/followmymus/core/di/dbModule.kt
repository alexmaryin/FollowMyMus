package io.github.alexmaryin.followmymus.core.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDatabase
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDbFactory
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.dao.MusicBrainzDAO
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

expect fun getDbMusicBrainzDbFactory(): MusicBrainzDbFactory

@Module
class DbModule() {

    @Single
    fun provideMusicBrainzDatabase(): MusicBrainzDatabase = getDbMusicBrainzDbFactory().create()
        .setDriver(BundledSQLiteDriver())
        .build()

    @Single
    fun provideMusicBrainzDao(database: MusicBrainzDatabase): MusicBrainzDAO = database.artistsDao
}