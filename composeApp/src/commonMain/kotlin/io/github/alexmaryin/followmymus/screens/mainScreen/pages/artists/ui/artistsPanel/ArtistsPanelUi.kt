package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel

import ErrorPlaceholder
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.ui.HandlePagingItems
import io.github.alexmaryin.followmymus.core.ui.ObserveEvents
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchError
import io.github.alexmaryin.followmymus.screens.commonUi.EmptyListPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListEvent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.SearchHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun ArtistsPanelUi(component: ArtistsList) {

    val state by component.state.subscribeAsState()
    val listState = rememberLazyListState()
    val artists = component.artists.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    var delayedFabVisible by remember { mutableStateOf(false) }

    val shouldShowFab by remember {
        derivedStateOf {
            // Show FAB only when: items exist, not scrolling, and list is not at top
            artists.itemCount > 0 &&
                    !listState.isScrollInProgress &&
                    (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        }
    }

    LaunchedEffect(shouldShowFab) {
        if (shouldShowFab) {
            delay(1.seconds)
            delayedFabVisible = true
        } else {
            delayedFabVisible = false
        }
    }

    // This effect added to prolong loading indicator until actual
    // fetching and mapping data from the flow or error occurs.
    LaunchedEffect(state.searchResultsCount, artists.loadState.refresh) {
        if (state.isLoading &&
            (state.searchResultsCount != null || artists.loadState.refresh is LoadState.Error)
        ) {
            component(ArtistsListAction.LoadingCompleted)
        }
    }

    ObserveEvents(component.events) { event ->
        when (event) {
            ArtistsListEvent.ScrollUp -> listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            HandlePagingItems(artists) {

                OnEmpty { EmptyListPlaceholder() }

                OnRefresh { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

                OnError { error ->
                    val errorText = when (error) {
                        SearchError.InvalidResponse ->
                            stringResource(Res.string.invalid_response_api)

                        is SearchError.NetworkError ->
                            if (error.message != null) {
                                stringResource(
                                    Res.string.network_error_msg,
                                    error.message
                                )
                            } else stringResource(Res.string.network_error)

                        is SearchError.ServerError ->
                            stringResource(
                                Res.string.server_error_code,
                                error.code, error.message
                            )
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

        AnimatedVisibility(
            visible = delayedFabVisible,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.padding(16.dp),
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_up),
                    contentDescription = "scroll up"
                )
            }
        }
    }
}
