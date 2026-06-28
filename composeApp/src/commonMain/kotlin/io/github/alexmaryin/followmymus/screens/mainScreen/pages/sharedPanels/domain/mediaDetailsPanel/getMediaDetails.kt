package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel

import com.arkivanov.decompose.ComponentContext

fun getMediaDetails(
    config: MediaDetailsConfig,
    context: ComponentContext,
): MediaDetails = MediaDetails(
    releaseId = config.releaseId,
    releaseName = config.releaseName,
    context = context,
)
