package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation

import io.github.alexmaryin.followmymus.preferences.DynamicMode
import io.github.alexmaryin.followmymus.preferences.Language
import io.github.alexmaryin.followmymus.preferences.ThemeMode

sealed interface AccountAction {
    data object ShowAbout : AccountAction
    data object ShowPrivacyPolicy : AccountAction
    data object ToggleQrView : AccountAction
    data object Logout : AccountAction
    data object LanguageClick : AccountAction
    data object ThemeClick : AccountAction
    data object DynamicClick : AccountAction
    data class LanguageChange(val language: Language) : AccountAction
    data class ThemeChange(val theme: ThemeMode) : AccountAction
    data class DynamicChange(val dynamicMode: DynamicMode) : AccountAction
    class DownloadQR(val image: ByteArray) : AccountAction
}