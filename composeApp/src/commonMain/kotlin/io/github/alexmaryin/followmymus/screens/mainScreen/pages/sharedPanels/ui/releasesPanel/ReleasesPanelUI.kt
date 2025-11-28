package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesList

@Composable
fun ReleasesPanelUi(component: ReleasesList) {
    val releases by component.releases.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        state = listState
    ) {
        items(releases.releases, key = { it.id }) { release ->
            ListItem(
                headlineContent = { Text(text = release.title) },
                overlineContent = { Text(text = release.disambiguation ?: "") },
                supportingContent = { Text(text = release.coverUrl ?: "") }
            )
        }
    }
}