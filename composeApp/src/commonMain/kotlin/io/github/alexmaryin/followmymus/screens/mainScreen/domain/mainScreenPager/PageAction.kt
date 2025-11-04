package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

sealed interface PageAction {
    data object Back : PageAction
}