package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteArtist(
    val id: String,
    val type: ArtistType,
    val name: String,
    val description: String? = null,
    val country: CountryISO? = null,
    val area: Area? = null,
    val beginArea: Area? = null,
    val lifeSpan: LifeSpan? = null,
    val tags: List<MusicTag> = emptyList(),
    val syncStatus: SyncStatus
)
