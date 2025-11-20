package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList

@OptIn(ExperimentalDecomposeApi::class)
interface FavoritesHostComponent : Page, BackHandlerOwner {

    val panels: Value<ChildPanels<*, FavoritesList, *, ReleasesList, *, MediaDetails>>

    override val state: Value<FavoritesHostState>
    operator fun invoke(action: FavoritesHostAction)
}