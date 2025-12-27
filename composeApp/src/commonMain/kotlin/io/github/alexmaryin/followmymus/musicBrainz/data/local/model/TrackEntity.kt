package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaItemId")]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val mediaId: String,
    val mediaItemId: String,
    val position: Int,
    val title: String,
    val lengthMs: Int?
)
