package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import com.arkivanov.decompose.value.Value

interface Page {

    val state: Value<PageState>

    operator fun invoke(action: PageAction)
}