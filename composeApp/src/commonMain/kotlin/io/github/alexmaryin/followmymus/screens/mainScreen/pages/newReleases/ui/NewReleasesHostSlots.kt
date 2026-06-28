@file:OptIn(com.arkivanov.decompose.ExperimentalDecomposeApi::class)

package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost.NewReleasesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation.NewReleasesHostComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

class NewReleasesHostSlots(
    private val component: NewReleasesHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent = @Composable {
        val panelsState by component.panels.subscribeAsState()
        val mediaPanel = panelsState.extra?.instance
        val mainPanel = panelsState.main.instance
        val title = mediaPanel?.scaffoldSlots?.titleContent ?: mainPanel.scaffoldSlots.titleContent
        title()
    }

    override val leadingIcon = @Composable {
        val state by component.state.subscribeAsState()
        if (state.backVisible) BackIcon { component(NewReleasesHostAction.OnBack) }
    }

    override val snackbarMessages: Flow<SnackbarMsg> =
        component.events.receiveAsFlow().distinctUntilChanged()
}
