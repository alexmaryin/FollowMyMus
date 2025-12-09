package io.github.alexmaryin.followmymus.core.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDatabase
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDbFactory
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
    fun provideArtistDao(database: MusicBrainzDatabase): ArtistDao = database.artistDao

    @Single
    fun provideTransactionalDao(database: MusicBrainzDatabase): TransactionalDao = database.transactionalDao

    @Single
    fun provideRelationDao(database: MusicBrainzDatabase): ArtistRelationsDao = database.artistRelationDao

    @Single
    fun provideSyncDao(database: MusicBrainzDatabase): SyncDao = database.syncDao

    @Single
    fun provideReleaseDao(database: MusicBrainzDatabase): ReleasesDao = database.releaseDao

    @Single
    fun provideResourceDao(database: MusicBrainzDatabase): ResourceDao = database.resourceDao

    @Single
    fun provideFavoriteDao(database: MusicBrainzDatabase): FavoriteDao = database.favoriteDao
}