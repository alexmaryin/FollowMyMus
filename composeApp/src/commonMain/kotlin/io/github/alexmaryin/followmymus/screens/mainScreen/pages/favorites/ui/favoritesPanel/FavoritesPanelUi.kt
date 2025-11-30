package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.ui.VinylLoadingIndicator
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.EmptyFavoritesPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem.FavoriteListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import io.github.alexmaryin.followmymus.screens.utils.DateCategory
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading -> {
                VinylLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center).size(100.dp)
                )
            }

            state.favoritesCount == 0 -> {
                EmptyFavoritesPlaceholder()
            }

            else -> {
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
                                val caption = when (key) {
                                    is SortKeyType.Abc -> key.letter

                                    is SortKeyType.Country -> {
                                        val resource = key.country.toLocalizedResourceName()
                                        stringResource(resource ?: Res.string.ISO_unknown)
                                    }

                                    is SortKeyType.Date -> when (key.date) {
                                        is DateCategory.ByYear -> key.date.year.toString()
                                        is DateCategory.Recent -> stringResource(key.date.type.titleRes)
                                    }

                                    is SortKeyType.Type -> key.value.name
                                    else -> ""
                                }
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
}