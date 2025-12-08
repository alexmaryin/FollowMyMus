package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalDecomposeApi::class)
class ArtistsHostSlots(
    private val component: ArtistsHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots  {

    override val snackbarMessages: Flow<SnackbarMsg> =
        component.events.receiveAsFlow().distinctUntilChanged()

    override val leadingIcon = @Composable {
        val state by component.state.subscribeAsState()
        if (state.backVisible) BackIcon { component(ArtistsHostAction.OnBack) }
    }

    override val titleContent = @Composable {
        val panelsState by component.panels.subscribeAsState()
        val releasesPanel = panelsState.details?.instance
        val mediaPanel = panelsState.extra?.instance
        val singleMode = panelsState.mode == ChildPanelsMode.SINGLE
        val title = when {
            mediaPanel != null -> mediaPanel.scaffoldSlots.titleContent
            singleMode && releasesPanel != null -> releasesPanel.scaffoldSlots.titleContent
            else -> panelsState.main.instance.scaffoldSlots.titleContent
        }
        title()
    }
}