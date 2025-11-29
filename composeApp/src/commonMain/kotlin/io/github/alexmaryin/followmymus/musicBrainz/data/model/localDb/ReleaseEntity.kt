package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType

@Entity
data class ReleaseEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val artistId: String,
    val title: String,
    val disambiguation: String? = null,
    val firstReleaseDate: String? = null,
    val primaryType: ReleaseType,
    val secondaryTypes: String? = null,
    val previewCoverUrl: String? = null,
    val fullCoverUrl: String? = null
)