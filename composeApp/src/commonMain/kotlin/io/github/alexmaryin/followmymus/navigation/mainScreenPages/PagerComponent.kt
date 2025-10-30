package io.github.alexmaryin.followmymus.navigation.mainScreenPages

import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value

interface PagerComponent {
    val pages: Value<ChildPages<*, Page>>
    val state: Value<MainScreenState>

    fun onAction(action: MainScreenAction)
}