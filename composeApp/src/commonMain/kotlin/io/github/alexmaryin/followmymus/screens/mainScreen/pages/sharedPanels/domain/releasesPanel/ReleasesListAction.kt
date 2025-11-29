package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

sealed interface ReleasesListAction {
    data class SelectRelease(val releaseId: String) : ReleasesListAction
}