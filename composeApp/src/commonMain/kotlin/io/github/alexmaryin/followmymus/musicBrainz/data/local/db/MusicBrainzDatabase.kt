package io.github.alexmaryin.followmymus.musicBrainz.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.*
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.convertors.InstantConverter

// TODO For release - remove all migrations and drop version to 1

@Database(
    entities = [
        AreaEntity::class,
        ArtistEntity::class,
        TagEntity::class,
        ResourceEntity::class,
        ReleaseEntity::class
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
@ConstructedBy(MusicBrainzDbConstructor::class)
abstract class MusicBrainzDatabase : RoomDatabase(){

    abstract val artistDao: ArtistDao
    abstract val artistRelationDao: ArtistRelationsDao
    abstract val transactionalDao: TransactionalDao
    abstract val releaseDao: ReleasesDao
    abstract val resourceDao: ResourceDao
    abstract val syncDao: SyncDao

    companion object {
        const val DB_NAME = "musicbrainz.db"
    }
}