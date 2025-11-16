package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.HasSearchBar
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.releasesPanel.ReleasesList

@OptIn(ExperimentalDecomposeApi::class)
interface ArtistsHostComponent : Page, BackHandlerOwner, HasSearchBar {

    val panels: Value<ChildPanels<*, ArtistsList, *, ReleasesList, *, MediaDetails>>

    override val state: Value<ArtistsHostState>
    operator fun invoke(action: ArtistsHostAction)
}