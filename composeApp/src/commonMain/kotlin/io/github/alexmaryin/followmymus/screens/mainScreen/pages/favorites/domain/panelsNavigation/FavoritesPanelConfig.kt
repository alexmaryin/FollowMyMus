package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import kotlinx.serialization.Serializable

@Serializable
sealed interface FavoritesPanelConfig {

    @Serializable
    data class ListConfig(val sortingType: SortArtists) : FavoritesPanelConfig

    @Serializable
    data class ReleasesConfig(val artistId: String) : FavoritesPanelConfig

    @Serializable
    data class MediaDetailsConfig(val releaseId: String) : FavoritesPanelConfig

    companion object {
        val SERIALIZERS = Triple(ListConfig.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())
    }
}