package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("artistId")]
)
data class ResourceEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val type: String,
    val url: String,
    val artistId: String
)
