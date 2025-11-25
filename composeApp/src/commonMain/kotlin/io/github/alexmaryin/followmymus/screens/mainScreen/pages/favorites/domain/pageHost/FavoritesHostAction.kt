@file:OptIn(ExperimentalDecomposeApi::class)

package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode

sealed interface FavoritesHostAction {
    data class ShowReleases(val artistId: String) : FavoritesHostAction
    data class ShowMediaDetails(val releaseId: String) : FavoritesHostAction
    data object CloseReleases : FavoritesHostAction
    data object CloseMediaDetails : FavoritesHostAction
    data class SetMode(val mode: ChildPanelsMode) : FavoritesHostAction
    data object SyncRequested : FavoritesHostAction
    data object OnBack : FavoritesHostAction
}