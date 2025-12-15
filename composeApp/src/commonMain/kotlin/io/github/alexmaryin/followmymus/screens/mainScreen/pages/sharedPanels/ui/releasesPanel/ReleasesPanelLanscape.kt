package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ArtistReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesColumnFlow

@Composable
fun ReleasesPanelLandscape(
    modifier: Modifier = Modifier,
    resources: Map<String, List<Resource>>,
    releases: Map<String, List<Release>>,
    actionHandler: (ReleasesListAction) -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ResourcesColumnFlow(modifier = modifier.weight(3f), resources = resources)
        ArtistReleasesList(modifier = modifier.weight(7f), releases = releases, actionHandler = actionHandler)
    }
}