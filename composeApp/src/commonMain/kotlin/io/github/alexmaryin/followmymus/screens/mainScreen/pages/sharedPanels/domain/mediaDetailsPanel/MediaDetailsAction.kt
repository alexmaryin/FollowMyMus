package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

sealed interface MediaDetailsAction {
    data object Close : MediaDetailsAction
}