package io.github.alexmaryin.followmymus.rootNavigation.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.alexmaryin.followmymus.core.ui.theme.FollowMyMusTheme
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent.Child
import io.github.alexmaryin.followmymus.screens.login.ui.LoginScreen
import io.github.alexmaryin.followmymus.screens.mainScreen.ui.MainScreen
import io.github.alexmaryin.followmymus.screens.signUp.ui.SignUpScreen
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen

@Composable
fun RootContent(component: RootComponent) = FollowMyMusTheme {
    Children(
        stack = component.childStack,
        animation = stackAnimation(slide() + fade())
    ) {
        when (val child = it.instance) {
            is Child.Splash -> SplashScreen()
            is Child.LoginChild -> LoginScreen(child.component)
            is Child.MainScreenPager -> MainScreen(child.component)
            is Child.SettingsChild -> {}
            is Child.SignUpChild -> SignUpScreen(child.component)
        }
    }
}