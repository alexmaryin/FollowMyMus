package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface FavoritesPanelConfig {

    @Serializable
    data object ListConfig : FavoritesPanelConfig

    @Serializable
    data class ReleasesConfig(val artistId: String, val artistName: String) : FavoritesPanelConfig

    @Serializable
    data class MediaDetailsConfig(val releaseId: String, val releaseName: String) : FavoritesPanelConfig

    companion object {
        val SERIALIZERS = Triple(ListConfig.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())
    }
}