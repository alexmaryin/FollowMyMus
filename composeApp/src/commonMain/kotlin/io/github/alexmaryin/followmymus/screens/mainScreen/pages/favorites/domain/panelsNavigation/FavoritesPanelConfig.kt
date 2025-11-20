package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@Serializable
sealed interface FavoritesPanelConfig {

    @Serializable
    data class ReleasesConfig(val artistId: String) : FavoritesPanelConfig

    @Serializable
    data class MediaDetailsConfig(val releaseId: String) : FavoritesPanelConfig

    companion object {
        val SERIALIZERS = Triple(Unit.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())
    }
}