package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.*
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.convertors.InstantConverter

@Database(
    entities = [
        AreaEntity::class,
        ArtistEntity::class,
        TagEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(InstantConverter::class)
@ConstructedBy(MusicBrainzDbConstructor::class)
abstract class MusicBrainzDatabase : RoomDatabase(){

    abstract val dao: MusicBrainzDAO

    companion object {
        const val DB_NAME = "musicbrainz.db"
    }
}