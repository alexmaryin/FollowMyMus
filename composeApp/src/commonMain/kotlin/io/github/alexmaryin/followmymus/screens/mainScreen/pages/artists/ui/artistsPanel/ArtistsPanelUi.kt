package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel

import ErrorPlaceholder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.core.ui.HandlePagingItems
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchError
import io.github.alexmaryin.followmymus.screens.commonUi.EmptyListPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.SearchHeader

@Composable
fun ArtistsPanelUi(component: ArtistsList) {

    val state by component.state.subscribeAsState()

    val listState = rememberLazyListState()

    val artists = state.artists.collectAsLazyPagingItems()

    // This effect added to prolong loading indicator until actual
    // fetching and mapping data from the flow.
    LaunchedEffect(state.searchResultsCount) {
        state.searchResultsCount?.let {
            component(ArtistsListAction.LoadingCompleted)
            listState.animateScrollToItem(0)
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        HandlePagingItems(artists) {
            OnEmpty {
                EmptyListPlaceholder()
            }
            OnRefresh {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            OnError { error ->
                val errorText = when (error) {
                    SearchError.InvalidResponse -> "Invalid response"
                    SearchError.NetworkError -> "Network error"
                    is SearchError.ServerError -> "Server error ${error.code}"
                }
                ErrorPlaceholder(text = errorText) {
                    component(ArtistsListAction.Retry)
                }
            }
            OnSuccess {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    state.searchResultsCount?.let {
                        item { SearchHeader(it) }
                    }
                    onPagingItems({ it.id }) { artist ->
                        ArtistListItem(artist, component::invoke)
                    }
                    onAppendItem { CircularProgressIndicator(Modifier.padding(6.dp)) }
                    onLastItem { }
                }
            }
        }
    }
}
