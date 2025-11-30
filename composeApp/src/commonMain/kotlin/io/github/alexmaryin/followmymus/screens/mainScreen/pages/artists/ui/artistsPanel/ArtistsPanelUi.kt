package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel

import ErrorPlaceholder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import io.github.alexmaryin.followmymus.core.ui.VinylLoadingIndicator
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchError
import io.github.alexmaryin.followmymus.screens.commonUi.EmptyListPlaceholder
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListEvent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistListItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components.ListHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistsPanelUi(component: ArtistsList) {

    val state by component.state.subscribeAsState()
    val listState = rememberLazyListState()
    val artists = component.artists.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    var isFabVisible by remember { mutableStateOf(false) }

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
            isFabVisible = true
        } else {
            isFabVisible = false
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
            VinylLoadingIndicator(
                modifier = Modifier.align(Alignment.Center).size(150.dp)
            )
        } else {
            HandlePagingItems(artists) {

                OnEmpty { EmptyListPlaceholder() }

                OnRefresh { VinylLoadingIndicator(modifier = Modifier.align(Alignment.Center).size(150.dp)) }

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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        state.searchResultsCount?.let {
                            item { ListHeader(stringResource(Res.string.artists_search_header, it)) }
                        }
                        onPagingItems({ it.id }) { artist ->
                            ArtistListItem(artist, component::invoke)
                        }
                        onAppendItem { LoadingIndicator(Modifier.padding(6.dp)) }
                        onLastItem { }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFabVisible,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(),
            exit = fadeOut()
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
