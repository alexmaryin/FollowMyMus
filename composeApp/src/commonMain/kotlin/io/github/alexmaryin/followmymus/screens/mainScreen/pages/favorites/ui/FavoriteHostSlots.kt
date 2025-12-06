package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar.Avatar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalDecomposeApi::class)
class FavoriteHostSlots(
    private val component: FavoritesHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val snackbarMessages: Flow<SnackbarMsg> =
        component.events.receiveAsFlow().distinctUntilChanged()

    override val leadingIcon = @Composable {
        val state by component.state.subscribeAsState()
        if (state.backVisible) BackIcon { component(FavoritesHostAction.OnBack) }
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

    override val trailingIcon: @Composable (RowScope.() -> Unit) = {
        val state by component.state.subscribeAsState()
        val avatarState = derivedStateOf { state.avatar }
        Avatar(
            state = avatarState.value,
            modifier = Modifier.padding(4.dp),
            onSyncRequest = { component(FavoritesHostAction.SyncRequested) }
        )
    }
}