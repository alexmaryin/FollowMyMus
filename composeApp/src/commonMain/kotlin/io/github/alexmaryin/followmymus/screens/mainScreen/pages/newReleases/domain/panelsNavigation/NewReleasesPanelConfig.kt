package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetailsConfig
import kotlinx.serialization.builtins.serializer

sealed interface NewReleasesPanelConfig {

    companion object {
        val SERIALIZERS = Triple(Unit.serializer(), MediaDetailsConfig.serializer(), Unit.serializer())
    }
}
