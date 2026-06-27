package io.github.alexmaryin.followmymus.core.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDatabase
import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDbFactory
import io.github.alexmaryin.followmymus.musicBrainz.data.repository.*
import io.github.alexmaryin.followmymus.musicBrainz.domain.*
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

expect fun getDbMusicBrainzDbFactory(): MusicBrainzDbFactory

@Module
class DbModule {

    @Single
    fun provideMusicBrainzDatabase(): MusicBrainzDatabase = getDbMusicBrainzDbFactory().create()
        .setDriver(BundledSQLiteDriver())
        .build()

    @Factory
    fun provideArtistsRepository(
        artistDao: ArtistDao,
        favoriteDao: FavoriteDao,
        supabaseDb: SupabaseDb,
        searchEngine: SearchEngine,
        dbRepository: LocalDbRepository
    ): ArtistsRepository = ApiArtistsRepository(artistDao, favoriteDao, supabaseDb, searchEngine, dbRepository)

    @Single
    fun provideReleasesRepository(
        searchEngine: SearchEngine,
        coversEngine: CoversEngine,
        releaseDao: ReleasesDao,
        resourceDao: ResourceDao,
        transactionalDao: TransactionalDao
    ): ReleasesRepository = ApiReleasesRepository(searchEngine, coversEngine, releaseDao, resourceDao, transactionalDao)

    @Single
    fun provideMediaRepository(
        searchEngine: SearchEngine,
        coversEngine: CoversEngine,
        mediaDao: MediaDao
    ): MediaRepository = ApiMediaRepository(searchEngine, coversEngine, mediaDao)

    @Factory
    fun provideSyncRepository(
        syncDao: SyncDao,
        supabaseDb: SupabaseDb,
        searchEngine: SearchEngine,
        dbRepository: LocalDbRepository
    ): SyncRepository = ApiSyncRepository(syncDao, supabaseDb, searchEngine, dbRepository)

    @Factory
    fun provideLocalDbRepository(
        transactionalDao: TransactionalDao,
        relationDao: ArtistRelationsDao,
        artistDao: ArtistDao,
        releasesDao: ReleasesDao,
        resourceDao: ResourceDao,
        mediaDao: MediaDao
    ): LocalDbRepository = RoomRepository(
        transactionalDao, relationDao, artistDao, releasesDao, resourceDao, mediaDao
    )

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
}