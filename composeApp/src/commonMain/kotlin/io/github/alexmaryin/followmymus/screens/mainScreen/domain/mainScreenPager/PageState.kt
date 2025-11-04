package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import kotlinx.serialization.Serializable

@Serializable
open class PageState(
    val isBackVisible: Boolean = false
)
