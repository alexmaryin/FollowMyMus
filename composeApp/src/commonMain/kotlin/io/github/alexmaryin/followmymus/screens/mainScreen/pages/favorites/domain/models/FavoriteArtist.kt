package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class FavoriteArtist(
    val id: String,
    val type: ArtistType,
    val name: String,
    val sortName: String,
    val description: String? = null,
    val country: CountryISO = CountryISO("unknown"),
    val area: Area? = null,
    val beginArea: Area? = null,
    val lifeSpan: LifeSpan? = null,
    val tags: List<MusicTag> = emptyList(),
    val syncStatus: SyncStatus,
    val createdAt: Instant
)
