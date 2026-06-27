package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import ErrorPlaceholder
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
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.favorite_artists_list_title
import followmymus.composeapp.generated.resources.network_error
import followmymus.composeapp.generated.resources.remove_artist_dialog_text
import followmymus.composeapp.generated.resources.remove_artist_dialog_title
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.core.paging.PagingError
import io.github.alexmaryin.followmymus.core.ui.HandlePagingItems
import io.github.alexmaryin.followmymus.core.ui.PullToRefreshMobile
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortKeyType
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.caption
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
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

    val groupedPagingItems = component.groupedFavoriteArtists.collectAsLazyPagingItems()

    if (state.isRemoveDialogVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_artist_dialog_title),
            text = stringResource(Res.string.remove_artist_dialog_text, state.artistToRemove?.name ?: ""),
            onConfirm = { component(FavoritesListAction.RemoveFromFavorite) },
            onDismiss = { component(FavoritesListAction.DismissRemoveDialog) }
        )
    }

    HandlePagingItems(
        items = groupedPagingItems,
        errorMapper = { PagingError.Unknown(it.message) }
    ) {

        OnError {
            ErrorPlaceholder(text = stringResource(Res.string.network_error)) {
                component(FavoritesListAction.Refresh)
            }
        }

        OnEmpty {
            EmptyFavoritesPlaceholder()
        }

        OnContent {
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
                        onPagingItems(
                            key = { item ->
                                when (item) {
                                    is GroupedItem.Header<*> -> "header_${item.key}"
                                    is GroupedItem.Item<*> -> {
                                        @Suppress("UNCHECKED_CAST")
                                        "artist_${(item.value as FavoriteArtist).id}"
                                    }
                                }
                            }
                        ) { item ->
                            when (item) {
                                is GroupedItem.Header<*> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    ListHeader((item.key as SortKeyType).caption())
                                }

                                is GroupedItem.Item<*> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val artist = item.value as FavoriteArtist
                                    FavoriteListItem(
                                        artist = artist,
                                        isSelected = state.selectedArtist == artist.id,
                                        onAction = component::invoke
                                    )
                                }
                            }
                        }
                        onAppendLoading { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}
