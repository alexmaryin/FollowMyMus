package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain

import io.github.alexmaryin.followmymus.preferences.Language
import io.github.alexmaryin.followmymus.preferences.ThemeMode
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageState
import kotlinx.serialization.Serializable

@Serializable
data class AccountPageState(
    val nickname: String = "",
    val language: Language = Language.SYSTEM,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val version: String = "",
    val deepLink: String? = null,
    val backVisible: Boolean = false,
    val isThemeModalOpened: Boolean = false,
    val isLanguageModalOpened: Boolean = false,
    val sessionLogout: Boolean = false,
) : PageState(backVisible)
