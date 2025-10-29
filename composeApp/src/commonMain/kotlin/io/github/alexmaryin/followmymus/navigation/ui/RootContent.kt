package io.github.alexmaryin.followmymus.navigation.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.alexmaryin.followmymus.navigation.RootComponent
import io.github.alexmaryin.followmymus.navigation.RootComponent.Child
import org.koin.compose.koinInject

@Composable
fun RootContent(component: RootComponent = koinInject()) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is Child.LoginChild -> TODO()
            is Child.MainScreenPager -> TODO()
            is Child.SettingsChild -> TODO()
            is Child.SignUpChild -> TODO()
        }
    }
}