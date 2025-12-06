package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import kotlinx.serialization.Serializable

@Serializable
data class ReleasesListState(
    val artistName: String,
    val isLoading: Boolean = false,
    val selectedRelease: String? = null,
    val openedCover: String? = null,
)
