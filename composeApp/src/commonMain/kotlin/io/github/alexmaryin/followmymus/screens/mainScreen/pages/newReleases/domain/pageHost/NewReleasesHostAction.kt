@file:OptIn(ExperimentalDecomposeApi::class)
package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode

sealed interface NewReleasesHostAction {
    data class ShowMediaDetails(val releaseId: String, val releaseName: String) : NewReleasesHostAction
    data object CloseMediaDetails : NewReleasesHostAction
    data class SetMode(val mode: ChildPanelsMode) : NewReleasesHostAction
    data object Refresh : NewReleasesHostAction
    data object OnBack : NewReleasesHostAction
}
