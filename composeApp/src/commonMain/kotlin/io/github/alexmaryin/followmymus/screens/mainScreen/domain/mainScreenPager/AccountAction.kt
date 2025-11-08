package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexmaryin.followmymus.preferences.Language
import io.github.alexmaryin.followmymus.preferences.ThemeMode

sealed class AccountAction : PageAction {
    data object ShowAbout : AccountAction()
    data object ShowPrivacyPolicy : AccountAction()
    data object ToggleQrView : AccountAction()
    data object Logout : AccountAction()
    data object LanguageClick : AccountAction()
    data object ThemeClick : AccountAction()
    data class LanguageChange(val language: Language) : AccountAction()
    data class ThemeChange(val theme: ThemeMode) : AccountAction()
    data class DownloadQR(val image: ImageBitmap) : AccountAction()
}