package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetailsConfig
import kotlinx.serialization.Serializable

@Serializable
sealed interface FavoritesPanelConfig {

    @Serializable
    data object ListConfig : FavoritesPanelConfig

    @Serializable
    data class ReleasesConfig(val artistId: String, val artistName: String) : FavoritesPanelConfig

    companion object {
        val SERIALIZERS = Triple(ListConfig.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())
    }
}