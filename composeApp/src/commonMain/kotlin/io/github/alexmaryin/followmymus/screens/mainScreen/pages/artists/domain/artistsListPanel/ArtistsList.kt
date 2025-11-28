package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import androidx.compose.runtime.Composable
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistsSearchBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ArtistsList(
    private val context: ComponentContext,
    private val hostAction: (ArtistsHostAction) -> Unit
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
    private val scope = context.coroutineScope() + SupervisorJob()

    private val _state by saveableMutableValue(ArtistsListState.serializer(), init = ::ArtistsListState)
    val state: Value<ArtistsListState> = _state

    private val pager = retainedInstance {
        ArtistsPager(repository)
    }
    val artists: Flow<PagingData<Artist>> = pager.artists

    private val _events = MutableSharedFlow<ArtistsListEvent>()
    val events = _events.asSharedFlow()

    init {
        // When the component is created/recreated, check if there's a query in the saved state and restore search.
        if (state.value.query.isNotBlank()) pager.search(state.value.query)

        scope.launch {
            repository.searchCount.collect { total ->
                _state.update { it.copy(searchResultsCount = total) }
            }
        }
    }

    operator fun invoke(action: ArtistsListAction) {
        when (action) {
            is ArtistsListAction.Search -> startSearch(action.query)
            is ArtistsListAction.ToggleArtistFavorite -> scope.launch { toggleFavorite(action.artist) }
            is ArtistsListAction.SelectArtist -> hostAction(ArtistsHostAction.ShowReleases(action.artistId))
            ArtistsListAction.ToggleSearchTune -> TODO()
            ArtistsListAction.Retry -> startSearch(state.value.query)
            ArtistsListAction.LoadingCompleted -> scope.launch {
                _state.update { it.copy(isLoading = false) }
                _events.emit(ArtistsListEvent.ScrollUp)
            }
        }
    }

    private fun startSearch(query: String) {
        if (query.isBlank()) return
        hostAction(ArtistsHostAction.CloseReleases)
        _state.update { it.copy(isLoading = true, searchResultsCount = null, query = query) }
        pager.search(query)
    }

    private suspend fun toggleFavorite(artist: Artist) {
        if (artist.isFavorite) {
            repository.deleteFromFavorites(artist.id)
        } else {
            repository.addToFavorite(artist)
        }
    }

    @Composable
    fun ProvideArtistsSearchBar() = ArtistsSearchBar(::invoke)

    @OptIn(ExperimentalCoroutinesApi::class)
    private class ArtistsPager(
        private val repository: ArtistsRepository
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val searchQuery = MutableSharedFlow<String>(replay = 1)

        val artists: Flow<PagingData<Artist>> = searchQuery
            .flatMapLatest { query -> repository.searchArtists(query) }
            .cachedIn(scope)
            .combine(repository.getFavoriteArtistsIds()) { pagingData, favoriteIds ->
                pagingData.map { artist ->
                    artist.copy(isFavorite = artist.id in favoriteIds)
                }
            }

        fun search(query: String) {
            searchQuery.tryEmit(query)
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}