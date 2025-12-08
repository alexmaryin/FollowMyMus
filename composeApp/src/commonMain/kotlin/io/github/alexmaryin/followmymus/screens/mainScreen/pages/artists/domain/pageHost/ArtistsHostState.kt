package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost

import kotlinx.serialization.Serializable

@Serializable
data class ArtistsHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
)
