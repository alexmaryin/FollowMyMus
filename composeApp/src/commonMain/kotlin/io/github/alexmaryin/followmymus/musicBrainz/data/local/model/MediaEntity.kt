package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseStatus

@Entity
data class MediaEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val releaseId: String,
    val title: String,
    val disambiguation: String?,
    val status: ReleaseStatus = ReleaseStatus.Official,
    val country: CountryISO,
    val date: String? = null,
    val barcode: String? = null,
    val quality: String? = null,
    val previewCoverUrl: String? = null,
    val fullCoverUrl: String? = null
)

data class MediaWithData(
    @Embedded val media: MediaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mediaId",
    ) val items: List<MediaItemEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "mediaId"
    ) val tracks: List<TrackEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "mediaId"
    ) val resources: List<MediaResourceEntity> = emptyList()
)
