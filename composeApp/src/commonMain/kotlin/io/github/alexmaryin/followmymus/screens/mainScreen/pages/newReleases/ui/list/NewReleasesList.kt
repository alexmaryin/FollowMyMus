package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.list

import ErrorPlaceholder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.core.ui.HandlePagingItems
import io.github.alexmaryin.followmymus.core.ui.PullToRefreshMobile
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.local.model.NewReleaseState
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SecondaryType
import io.github.alexmaryin.followmymus.screens.commonUi.EmptyListPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Release
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ReleaseListItem
import kotlinx.datetime.LocalDate

@Composable
fun NewReleasesList(component: NewReleasesList) {
    val state by component.state.subscribeAsState()
    val items = component.releases.collectAsLazyPagingItems()

    HandlePagingItems(items = items) {
        OnEmpty { EmptyListPlaceholder(modifier = Modifier.fillMaxSize()) }
        OnError { error ->
            ErrorPlaceholder(text = error.toString())
        }
        OnContent { paged ->
            PullToRefreshMobile(
                isRefreshing = state.isLoading,
                onRefresh = { component(NewReleasesListAction.LoadFromRemote) },
                modifier = Modifier.fillMaxSize()
            ) {
                NewReleasesListContent(
                    releases = paged,
                    onSelect = { id, name -> component(NewReleasesListAction.SelectRelease(id, name)) },
                    onDismiss = { id -> component(NewReleasesListAction.Dismiss(id)) }
                )
            }
        }
    }
}

@Composable
private fun NewReleasesListContent(
    releases: LazyPagingItems<GroupedItem<NewReleaseEntity, String>>,
    onSelect: (releaseId: String, releaseName: String) -> Unit,
    onDismiss: (releaseId: String) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        items(
            count = releases.itemCount,
            key = releases.itemKey { item ->
                when (item) {
                    is GroupedItem.Header -> "header_${item.key}"
                    is GroupedItem.Item -> "release_${item.value.id}"
                }
            }
        ) { index ->
            when (val item = releases[index]) {
                is GroupedItem.Header -> Text(
                    text = item.key,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                )
                is GroupedItem.Item -> SwipeableNewRelease(
                    entity = item.value,
                    onSelect = onSelect,
                    onDismiss = onDismiss
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun SwipeableNewRelease(
    entity: NewReleaseEntity,
    onSelect: (releaseId: String, releaseName: String) -> Unit,
    onDismiss: (releaseId: String) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(entity.state) {
        if (entity.state != NewReleaseState.DISMISSED) dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    val adapter: (ReleasesListAction) -> Unit = { action ->
        when (action) {
            is ReleasesListAction.SelectRelease -> onSelect(action.releaseId, action.releaseName)
            else -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { /* tinted background only; v1 is no-op */ },
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart ||
                it == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDismiss(entity.id)
            }
        },
    ) {
        ReleaseListItem(
            release = entity.toRelease(),
            actionHandler = adapter,
            unseen = entity.state == NewReleaseState.UNSEEN,
            showType = true
        )
    }
}

private fun NewReleaseEntity.toRelease(): Release = Release(
    id = id,
    title = title,
    disambiguation = disambiguation,
    firstReleaseDate = firstReleaseDate?.let(LocalDate::parse),
    primaryType = primaryType,
    secondaryTypes = secondaryTypes
        ?.split(",")
        ?.mapNotNull { token -> runCatching { SecondaryType.valueOf(token) }.getOrNull() }
        ?: emptyList(),
    previewCoverUrl = coverFrontUrl,
    largeCoverUrl = null
)
