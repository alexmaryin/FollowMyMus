package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ArtistReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.CoverView
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesFlow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReleasesPanelUi(component: ReleasesList) {

    val resources = component.resources.collectAsStateWithLifecycle(emptyMap())
    val releases = component.releases.collectAsStateWithLifecycle(emptyMap())

    val state by component.state.subscribeAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        Column {
            ResourcesFlow(resources.value)
            ArtistReleasesList(releases.value, component::invoke)
        }
    }

    state.openedCover?.let {
        CoverView(it) { component(ReleasesListAction.CloseFullCover) }
    }
}
