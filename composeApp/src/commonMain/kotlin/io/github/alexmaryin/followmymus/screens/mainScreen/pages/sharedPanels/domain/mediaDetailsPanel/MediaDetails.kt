package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import com.arkivanov.decompose.ComponentContext
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import kotlinx.coroutines.channels.Channel

class MediaDetails(
    private val releaseId: String,
    private val context: ComponentContext
) : Page, ComponentContext by context {
    override val scaffoldSlots = DefaultScaffoldSlots
    override val events = Channel<SnackbarMsg>()
    override val key = "MediaDetails"


}