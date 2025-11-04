package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

sealed class AccountAction : PageAction {
    data object ShowAbout : AccountAction()
    data object ShowPrivacyPolicy : AccountAction()
}