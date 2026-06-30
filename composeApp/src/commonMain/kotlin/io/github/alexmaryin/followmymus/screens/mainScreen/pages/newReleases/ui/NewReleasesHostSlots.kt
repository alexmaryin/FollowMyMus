@file:OptIn(com.arkivanov.decompose.ExperimentalDecomposeApi::class)

package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.more_vert
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost.NewReleasesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation.NewReleasesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.list.NewReleasesActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.painterResource

class NewReleasesHostSlots(
    private val component: NewReleasesHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent: @Composable () -> Unit = {
        val panelsState by component.panels.subscribeAsState()
        val mediaPanel = panelsState.details?.instance
        val mainPanel = panelsState.main.instance
        val title = mediaPanel?.scaffoldSlots?.titleContent ?: mainPanel.scaffoldSlots.titleContent
        title()
    }

    override val leadingIcon = @Composable {
        val state by component.state.subscribeAsState()
        if (state.backVisible) BackIcon { component(NewReleasesHostAction.OnBack) }
    }

    override val trailingIcon: @Composable RowScope.() -> Unit = {
        val panelsState by component.panels.subscribeAsState()
        val list = panelsState.main.instance
        val state by list.state.subscribeAsState()
        var showActions by remember { mutableStateOf(false) }

        IconButton(onClick = { showActions = true }) {
            Icon(
                painter = painterResource(Res.drawable.more_vert),
                contentDescription = "Release actions",
            )
        }

        NewReleasesActions(
            expanded = showActions,
            hasDismissals = state.hasDismissals,
            onDismiss = { showActions = false },
            onAction = list::invoke,
        )
    }

    override val snackbarMessages: Flow<SnackbarMsg> =
        component.events.receiveAsFlow().distinctUntilChanged()
}
