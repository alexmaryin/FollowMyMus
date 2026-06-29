package io.github.alexmaryin.followmymus.core.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDatabase
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDbFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

expect fun getDbMusicBrainzDbFactory(): MusicBrainzDbFactory

@Module
@ComponentScan("io.github.alexmaryin.followmymus")
class DbModule {

    @Single
    fun provideMusicBrainzDatabase(): MusicBrainzDatabase = getDbMusicBrainzDbFactory().create()
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()

    @Factory
    fun provideArtistDao(database: MusicBrainzDatabase): ArtistDao = database.artistDao

    @Factory
    fun provideTransactionalDao(database: MusicBrainzDatabase): TransactionalDao = database.transactionalDao

    @Factory
    fun provideRelationDao(database: MusicBrainzDatabase): ArtistRelationsDao = database.artistRelationDao

    @Factory
    fun provideSyncDao(database: MusicBrainzDatabase): SyncDao = database.syncDao

    @Factory
    fun provideReleaseDao(database: MusicBrainzDatabase): ReleasesDao = database.releaseDao

    @Factory
    fun provideResourceDao(database: MusicBrainzDatabase): ResourceDao = database.resourceDao

    @Factory
    fun provideFavoriteDao(database: MusicBrainzDatabase): FavoriteDao = database.favoriteDao

    @Factory
    fun provideMediaDao(database: MusicBrainzDatabase): MediaDao = database.mediaDao

    @Factory
    fun provideNewReleasesDao(database: MusicBrainzDatabase): NewReleasesDao = database.newReleasesDao
}
