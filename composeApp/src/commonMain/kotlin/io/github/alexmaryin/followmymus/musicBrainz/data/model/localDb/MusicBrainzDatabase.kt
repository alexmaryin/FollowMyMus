package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AreaEntity::class,
        ArtistEntity::class,
        TagEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(MusicBrainzDbConstructor::class)
abstract class MusicBrainzDatabase : RoomDatabase(){

    abstract val dao: MusicBrainzDAO

    companion object {
        const val DB_NAME = "musicbrainz.db"
    }
}