package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction

@Composable
fun ArtistReleasesList(
    modifier: Modifier = Modifier,
    releases: LazyPagingItems<GroupedItem<Release, String>>,
    actionHandler: (ReleasesListAction) -> Unit
) {
    val releasesState = rememberLazyListState()

    LazyColumn(state = releasesState, modifier = modifier) {
        items(
            count = releases.itemCount,
            key = releases.itemKey { item ->
                when (item) {
                    is GroupedItem.Header -> item.key
                    is GroupedItem.Item -> {
                        "release_${item.value.id}"
                    }
                }
            }
        ) { index ->
            when (val item = releases[index]) {
                is GroupedItem.Header -> {
                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    )
                }
                is GroupedItem.Item -> {
                    ReleaseListItem(item.value, actionHandler)
                }
                else -> Unit
            }
        }
    }
}
