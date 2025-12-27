package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaId")]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val mediaId: String,
    val position: Int,
    val format: String,
    val title: String,
    val trackCount: Int
)

