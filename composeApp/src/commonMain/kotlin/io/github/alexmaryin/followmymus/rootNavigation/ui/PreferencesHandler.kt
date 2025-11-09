package io.github.alexmaryin.followmymus.rootNavigation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexmaryin.followmymus.core.changeLanguage
import io.github.alexmaryin.followmymus.core.ui.isAndroid
import io.github.alexmaryin.followmymus.preferences.*
import io.github.alexmaryin.followmymus.rootNavigation.RootAction
import io.github.alexmaryin.followmymus.rootNavigation.RootState

@Composable
fun PreferencesHandler(
    state: RootState,
    actionHandler: (RootAction) -> Unit
) {
    val datastore = rememberPrefs()
    val preferences = rememberAppPreferences(datastore)

    val theme by preferences.getThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val systemDark = isSystemInDarkTheme()
    var isDark by remember { mutableStateOf(state.isDark) }

    val androidDynamicMode by preferences.getAndroidDynamicMode().collectAsStateWithLifecycle(DynamicMode.ON)
    var dynamicMode by remember { mutableStateOf(state.dynamicMode) }

    val language by preferences.getLanguage().collectAsStateWithLifecycle(Language.SYSTEM)
    var appLanguageTag by remember { mutableStateOf(state.languageTag) }


    LaunchedEffect(theme, androidDynamicMode, language) {
        val newIsDark = theme == ThemeMode.DARK || (theme == ThemeMode.SYSTEM && systemDark)
        if (isDark != newIsDark) {
            actionHandler(RootAction.ChangeTheme(newIsDark))
            isDark = newIsDark
        }

        val newDynamicMode = isAndroid() && androidDynamicMode == DynamicMode.ON
        if (dynamicMode != newDynamicMode) {
            actionHandler(RootAction.ChangeDynamicMode(newDynamicMode))
            dynamicMode = newDynamicMode
        }

        val newLanguage = when (language) {
            Language.ENGLISH -> "en"
            Language.RUSSIAN -> "ru"
            Language.SYSTEM -> null
        }
        if (appLanguageTag != newLanguage) {
            changeLanguage(newLanguage)
            actionHandler(RootAction.ChangeLanguage(newLanguage))
            appLanguageTag = newLanguage
        }
    }
}