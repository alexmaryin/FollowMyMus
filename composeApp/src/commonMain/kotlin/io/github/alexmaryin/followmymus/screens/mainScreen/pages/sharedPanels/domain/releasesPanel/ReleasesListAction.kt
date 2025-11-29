package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

sealed interface ReleasesListAction {
    data class SelectRelease(val releaseId: String) : ReleasesListAction
    data class OpenFullCover(val coverUrl: String) : ReleasesListAction
    data object CloseFullCover : ReleasesListAction
}