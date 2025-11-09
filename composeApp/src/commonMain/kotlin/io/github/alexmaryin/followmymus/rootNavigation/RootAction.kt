package io.github.alexmaryin.followmymus.rootNavigation

sealed class RootAction {
    data class ChangeTheme(val isDark: Boolean) : RootAction()
    data class ChangeLanguage(val languageTag: String?) : RootAction()
    data class ChangeDynamicMode(val dynamicMode: Boolean) : RootAction()
}