package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@Serializable
sealed interface ArtistsPanelConfig {

    @Serializable
    data class ReleasesConfig(val artistId: String, val artistName: String) : ArtistsPanelConfig

    @Serializable
    data class MediaDetailsConfig(val releaseId: String) : ArtistsPanelConfig

    companion object {
        val SERIALIZERS = Triple(Unit.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())
    }
}