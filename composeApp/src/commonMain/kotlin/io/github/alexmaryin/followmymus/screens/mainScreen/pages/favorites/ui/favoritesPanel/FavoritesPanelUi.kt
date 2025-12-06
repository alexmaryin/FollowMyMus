package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.favorite_artists_list_title
import followmymus.composeapp.generated.resources.remove_artist_dialog_text
import followmymus.composeapp.generated.resources.remove_artist_dialog_title
import io.github.alexmaryin.followmymus.core.ui.PullToRefreshMobile
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.caption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.EmptyFavoritesPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem.FavoriteListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesPanelUi(component: FavoritesList) {
    val state by component.state.subscribeAsState()
    val listState = rememberLazyListState()
    val favoriteArtists by component.favoriteArtists.collectAsStateWithLifecycle(emptyMap())


    if (state.isRemoveDialogVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_artist_dialog_title),
            text = stringResource(Res.string.remove_artist_dialog_text, state.artistToRemove?.name ?: ""),
            onConfirm = { component(FavoritesListAction.RemoveFromFavorite) },
            onDismiss = { component(FavoritesListAction.DismissRemoveDialog) }
        )
    }

    if (state.favoritesCount == 0) {
        EmptyFavoritesPlaceholder()
    } else {
        PullToRefreshMobile(
            isRefreshing = state.isLoading,
            onRefresh = { component(FavoritesListAction.Refresh) }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
            ) {
                item {
                    ListHeader(
                        stringResource(
                            Res.string.favorite_artists_list_title, state.favoritesCount
                        )
                    )
                }

                favoriteArtists.forEach { (key, favoriteArtists) ->
                    if (key !is SortKeyType.None) {
                        stickyHeader(key = key.key) {
                            val caption = key.caption()
                            ListHeader(caption)
                        }
                    }

                    items(favoriteArtists, key = { it.id }) { artist ->
                        FavoriteListItem(
                            artist = artist,
                            isSelected = state.selectedArtist == artist.id,
                            onAction = component::invoke
                        )
                    }
                }
            }
        }
    }
}