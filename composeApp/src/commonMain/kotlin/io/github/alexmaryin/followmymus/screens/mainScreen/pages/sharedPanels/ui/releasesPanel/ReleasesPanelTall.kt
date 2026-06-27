package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ArtistReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ResourcesRowFlow

@Composable
fun ReleasesPanelTall(
    modifier: Modifier = Modifier,
    resources: Map<String, List<Resource>>,
    releases: LazyPagingItems<GroupedItem<Release, String>>,
    actionHandler: (ReleasesListAction) -> Unit
) {
    Column(modifier = modifier) {
        ResourcesRowFlow(resources = resources)
        ArtistReleasesList(releases = releases, actionHandler = actionHandler)
    }
}
