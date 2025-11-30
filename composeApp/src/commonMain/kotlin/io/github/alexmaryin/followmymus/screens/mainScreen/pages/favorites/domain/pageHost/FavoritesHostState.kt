package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import kotlinx.serialization.Serializable

@Serializable
data class FavoritesHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
)
