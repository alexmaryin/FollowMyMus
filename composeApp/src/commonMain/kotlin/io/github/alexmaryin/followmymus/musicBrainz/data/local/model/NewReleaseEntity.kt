package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import kotlin.time.Instant

/**
 * Local representation of a release-group surfaced on the New Releases page.
 *
 * `secondaryTypes` is stored as a comma-joined string (matching
 * [ReleaseEntity.secondaryTypes]) to avoid introducing a new [androidx.room.TypeConverter].
 * The corresponding comma-split happens at the entity-to-UI boundary.
 */
@Entity(tableName = "new_releases")
data class NewReleaseEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    val disambiguation: String? = null,
    val firstReleaseDate: String? = null,
    val primaryType: ReleaseType,
    val secondaryTypes: String? = null,
    val coverFrontUrl: String? = null,
    val state: NewReleaseState = NewReleaseState.UNSEEN,
    val discoveredAt: Instant,
)
