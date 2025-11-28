package io.github.alexmaryin.followmymus.screens.mainScreen.domain

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPages

data class MainScreenState(
    val nickname: String = "",
    val activePageIndex: Int = MainPages.FAVORITES.index
)