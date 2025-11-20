package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus

@Entity
data class ArtistEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val type: ArtistType?,
    val name: String,
    val sortName: String,
    val country: CountryISO?,
    val areaId: String?,
    val beginAreaId: String?,
    val disambiguation: String?,
    @Embedded(prefix = "artist_lifespan_")
    val lifeSpan: LifeSpanEntity?,
    val isFavorite: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PendingRemoteAdd
)
