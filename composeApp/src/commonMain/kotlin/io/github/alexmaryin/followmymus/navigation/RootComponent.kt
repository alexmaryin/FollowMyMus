package io.github.alexmaryin.followmymus.navigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import io.github.alexmaryin.followmymus.navigation.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.settings.domain.SettingsComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent

interface RootComponent {

    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data object Splash : Child()
        class LoginChild(val component: LoginComponent) : Child()
        class SignUpChild(val component: SignUpComponent) : Child()
        class SettingsChild(val component: SettingsComponent) : Child()
        class MainScreenPager(val component: PagerComponent) : Child()
    }
}