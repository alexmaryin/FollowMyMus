package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

sealed class AccountAction : PageAction {
    data object ShowAbout : AccountAction()
    data object ShowPrivacyPolicy : AccountAction()
    data object ToggleQrView : AccountAction()
    data object Logout : AccountAction()
    data object LanguageClick : AccountAction()
    data object ThemeClick : AccountAction()
}