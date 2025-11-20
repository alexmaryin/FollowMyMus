package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode

@OptIn(ExperimentalDecomposeApi::class)
sealed interface ArtistsHostAction {
    data class ShowReleases(val artistId: String) : ArtistsHostAction
    data class ShowMediaDetails(val releaseId: String): ArtistsHostAction
    data object CloseMediaDetails : ArtistsHostAction
    data class SetMode(val mode: ChildPanelsMode) : ArtistsHostAction
}