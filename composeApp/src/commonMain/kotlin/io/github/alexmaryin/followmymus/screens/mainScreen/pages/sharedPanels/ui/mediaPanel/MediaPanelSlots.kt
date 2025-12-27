package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

class ReleasePanelSlots(
    private val component: MediaDetails
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val snackbarMessages = component.events.receiveAsFlow().distinctUntilChanged()

    override val titleContent = @Composable {
        Text(
            component.state.value.releaseName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(4.dp)
        )
    }
}