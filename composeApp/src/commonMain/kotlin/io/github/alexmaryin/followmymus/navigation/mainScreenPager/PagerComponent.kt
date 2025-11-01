package io.github.alexmaryin.followmymus.navigation.mainScreenPager

import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenState

interface PagerComponent {
    val pages: Value<ChildPages<*, Page>>
    val state: Value<MainScreenState>

    fun onAction(action: MainScreenAction)
}