package io.github.alexmaryin.followmymus.rootNavigation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.alexmaryin.followmymus.core.changeLanguage
import io.github.alexmaryin.followmymus.core.ui.isDesktop
import io.github.alexmaryin.followmymus.core.ui.theme.FollowMyMusTheme
import io.github.alexmaryin.followmymus.preferences.Language
import io.github.alexmaryin.followmymus.preferences.ThemeMode
import io.github.alexmaryin.followmymus.preferences.rememberAppPreferences
import io.github.alexmaryin.followmymus.preferences.rememberPrefs
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent.Child
import io.github.alexmaryin.followmymus.screens.login.ui.LoginScreen
import io.github.alexmaryin.followmymus.screens.mainScreen.ui.MainScreen
import io.github.alexmaryin.followmymus.screens.signUp.ui.SignUpScreen
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RootContent(component: RootComponent) {

    val datastore = rememberPrefs()
    val preferences = rememberAppPreferences(datastore)
    val theme by preferences.getThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val isDark = theme == ThemeMode.DARK || (theme == ThemeMode.SYSTEM && isSystemInDarkTheme())
    var appLanguage by remember { mutableStateOf(Language.SYSTEM) }

    LaunchedEffect(Unit) {
        preferences.getLanguage().collectLatest { language ->
            val lang = when (language) {
                Language.ENGLISH -> "en"
                Language.RUSSIAN -> "ru"
                Language.SYSTEM -> null
            }
            changeLanguage(lang)
            appLanguage = language
        }
    }

    FollowMyMusTheme(darkTheme = isDark) {
        AddOnlyDesktopLanguageKey(appLanguage) {
            Children(
                stack = component.childStack,
                animation = stackAnimation(slide() + fade())
            ) {
                when (val child = it.instance) {
                    is Child.Splash -> SplashScreen()
                    is Child.LoginChild -> LoginScreen(child.component)
                    is Child.MainScreenPager -> MainScreen(child.component)
                    is Child.SignUpChild -> SignUpScreen(child.component)
                }
            }
        }
    }
}

@Composable
inline fun AddOnlyDesktopLanguageKey(key: Any?, content: @Composable () -> Unit) =
    if (isDesktop()) key(key) { content() } else content()