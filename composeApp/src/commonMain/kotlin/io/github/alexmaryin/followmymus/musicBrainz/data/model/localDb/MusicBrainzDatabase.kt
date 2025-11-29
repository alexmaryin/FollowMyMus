package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.convertors.InstantConverter
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.dao.MusicBrainzDAO

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

    abstract val artistsDao: MusicBrainzDAO

    companion object {
        const val DB_NAME = "musicbrainz.db"
    }
}