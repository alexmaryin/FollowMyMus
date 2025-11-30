package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import com.arkivanov.decompose.ComponentContext
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots

class MediaDetails(
    private val releaseId: String,
    private val context: ComponentContext
) : ComponentContext by context, ScaffoldSlots by DefaultScaffoldSlots {
}