package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent

@OptIn(ExperimentalDecomposeApi::class)
class ArtistsHostSlots(
    private val component: ArtistsHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots  {


    override val leadingIcon = @Composable {
        val state = component.state.subscribeAsState()
        if (state.value.backVisible) BackIcon { component(ArtistsHostAction.OnBack) }
    }

    override val titleContent = @Composable {
        val panelState by component.panels.subscribeAsState()
        val isSearchVisible = panelState.mode != ChildPanelsMode.SINGLE || panelState.details == null
        if (isSearchVisible) panelState.main.instance.scaffoldSlots.titleContent()
        else DefaultScaffoldSlots.titleContent()
    }
}