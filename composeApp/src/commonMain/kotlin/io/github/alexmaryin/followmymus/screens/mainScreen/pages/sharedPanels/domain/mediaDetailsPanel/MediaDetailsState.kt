package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import kotlinx.serialization.Serializable

@Serializable
data class MediaDetailsState(
    val releaseName: String,
    val isLoading: Boolean = false,
)
