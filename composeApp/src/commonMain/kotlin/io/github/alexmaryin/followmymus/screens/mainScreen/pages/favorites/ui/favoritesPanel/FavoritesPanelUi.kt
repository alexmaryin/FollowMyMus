package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.favorite_artists_list_title
import followmymus.composeapp.generated.resources.remove_artist_dialog_text
import followmymus.composeapp.generated.resources.remove_artist_dialog_title
import io.github.alexmaryin.followmymus.core.ui.PullToRefreshMobile
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.caption
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.EmptyFavoritesPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem.FavoriteListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesPanelUi(component: FavoritesList) {
    val state by component.state.subscribeAsState()
    val listState = rememberLazyListState()
    val hideHeader by derivedStateOf { listState.firstVisibleItemIndex > 1 }

    // Collect grouped paging data as lazy paging items
    val groupedPagingItems = component.groupedFavoriteArtists
        .collectAsLazyPagingItems()

    if (state.isRemoveDialogVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_artist_dialog_title),
            text = stringResource(Res.string.remove_artist_dialog_text, state.artistToRemove?.name ?: ""),
            onConfirm = { component(FavoritesListAction.RemoveFromFavorite) },
            onDismiss = { component(FavoritesListAction.DismissRemoveDialog) }
        )
    }

    // Handle loading states
    val isLoading = groupedPagingItems.loadState.refresh is LoadState.Loading || state.isLoading

    if (state.favoritesCount == 0 && !isLoading) {
        EmptyFavoritesPlaceholder()
    } else {
        PullToRefreshMobile(
            isRefreshing = state.isLoading,
            onRefresh = { component(FavoritesListAction.Refresh) }
        ) {
            Column {
                AnimatedVisibility(!hideHeader) {
                    val headerCaption = stringResource(
                        Res.string.favorite_artists_list_title,
                        state.favoritesCount
                    )
                    ListHeader(headerCaption)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                ) {
                    items(
                        count = groupedPagingItems.itemCount,
                        key = groupedPagingItems.itemKey { item ->
                            when (item) {
                                is GroupedFavoriteItem.Header -> item.uniqueId
                                is GroupedFavoriteItem.Artist -> "artist_${item.artist.id}"
                            }
                        }
                    ) { index ->
                        when (val item = groupedPagingItems[index]) {
                            is GroupedFavoriteItem.Header -> {
                                ListHeader(item.key.caption())
                            }

                            is GroupedFavoriteItem.Artist -> {
                                FavoriteListItem(
                                    artist = item.artist,
                                    isSelected = state.selectedArtist == item.artist.id,
                                    onAction = component::invoke
                                )
                            }

                            else -> {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}