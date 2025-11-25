package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots

interface Page : ScaffoldSlots {
    val key: String
}