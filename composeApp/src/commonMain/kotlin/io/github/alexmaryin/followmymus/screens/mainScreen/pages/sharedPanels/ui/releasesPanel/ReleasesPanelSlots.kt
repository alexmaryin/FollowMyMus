package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

class ReleasesPanelSlots(
    private val component: ReleasesList
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val snackbarMessages = component.events.receiveAsFlow().distinctUntilChanged()

    override val titleContent = @Composable {
        val state by component.state.subscribeAsState()
        ReleasesListTitle(
            artistName = state.artistName,
            actionsHandler = component::invoke
        )
    }
}