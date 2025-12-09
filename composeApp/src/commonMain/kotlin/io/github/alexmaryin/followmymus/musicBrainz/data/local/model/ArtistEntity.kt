package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.*
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import kotlin.time.Instant

@Entity(
    indices = [
        Index("sortName"),
        Index("country"),
    ]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val type: ArtistType?,
    val name: String,
    val sortName: String,
    val country: CountryISO = CountryISO("unknown"),
    val areaId: String?,
    val beginAreaId: String?,
    val disambiguation: String?,
    @Embedded(prefix = "artist_lifespan_")
    val lifeSpan: LifeSpanEntity?,
    val isFavorite: Boolean = false,
    val syncStatus: SyncStatus,
    @ColumnInfo(defaultValue = "1970-01-01T00:00:00Z")
    val createdAt: Instant
)
