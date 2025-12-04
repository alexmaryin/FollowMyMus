package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.core.ui.PullToRefreshMobile
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ArtistReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.CoverView
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesFlow

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReleasesPanelUi(component: ReleasesList) {

    val resources = component.resources.collectAsStateWithLifecycle(emptyMap())
    val releases = component.releases.collectAsStateWithLifecycle(emptyMap())

    val state by component.state.subscribeAsState()

    PullToRefreshMobile(
        isRefreshing = state.isLoading,
        onRefresh = { component(ReleasesListAction.LoadFromRemote) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            ResourcesFlow(resources.value)
            ArtistReleasesList(releases.value, component::invoke)
        }
    }

    state.openedCover?.let {
        CoverView(it) { component(ReleasesListAction.CloseFullCover) }
    }
}
