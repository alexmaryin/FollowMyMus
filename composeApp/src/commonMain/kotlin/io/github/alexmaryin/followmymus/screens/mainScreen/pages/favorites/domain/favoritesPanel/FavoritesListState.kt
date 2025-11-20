package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import kotlinx.serialization.Serializable

@Serializable
data class FavoritesListState(
    val isLoading: Boolean = false,
    val favoritesCount: Int = 0,
    val isSyncing: Boolean = false
)
