package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page

interface AccountHostComponent : Page {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data object Account : Child()
        data object PrivacyPolicy : Child()
        data object About : Child()
    }
}