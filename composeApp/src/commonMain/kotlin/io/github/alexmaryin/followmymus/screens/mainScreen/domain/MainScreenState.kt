package io.github.alexmaryin.followmymus.screens.mainScreen.domain

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPages

data class MainScreenState(
    val nickname: String = "",
    val backIconVisible: Boolean = false,
    val activePageIndex: Int = MainPages.RELEASES.index
)