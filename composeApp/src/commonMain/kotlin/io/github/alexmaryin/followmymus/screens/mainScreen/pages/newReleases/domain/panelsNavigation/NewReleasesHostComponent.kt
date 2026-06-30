package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost.NewReleasesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost.NewReleasesHostState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails

@OptIn(ExperimentalDecomposeApi::class)
interface NewReleasesHostComponent : Page, BackHandlerOwner {

    override val key get() = "NewReleasesHost"

    val panels: Value<ChildPanels<*, NewReleasesList, *, MediaDetails, *, Unit>>
    val state: Value<NewReleasesHostState>
    operator fun invoke(action: NewReleasesHostAction)
}
