package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import kotlinx.coroutines.channels.Channel

interface Page {
    val scaffoldSlots: ScaffoldSlots
    val events: Channel<SnackbarMsg>
    val key: String
}