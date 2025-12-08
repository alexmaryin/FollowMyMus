package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

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
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.ArtistsPanelSlots
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent

class ArtistsList(
    private val repository: ArtistsRepository,
    private val context: ComponentContext,
    private val hostAction: (ArtistsHostAction) -> Unit
) : Page, ComponentContext by context, KoinComponent {

    override val key = "ArtistsList"
    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = ArtistsPanelSlots(this)
    private val scope = context.coroutineScope()

    private val _state by saveableMutableValue(ArtistsListState.serializer(), init = ::ArtistsListState)
    val state: Value<ArtistsListState> = _state

    private val pager = retainedInstance {
        ArtistsPager(repository)
    }
    val artists: Flow<PagingData<Artist>> = pager.artists

    private val _listEvents = MutableSharedFlow<ArtistsListEvent>()
    val listEvents = _listEvents.asSharedFlow()

    init {
        // When the component is created/recreated, check if there's a query in the saved state and restore search.
        if (state.value.query.isNotBlank()) pager.search(state.value.query)
        else hostAction(ArtistsHostAction.CloseReleases)

        scope.launch {
            repository.searchCount.collect { total ->
                _state.update { it.copy(searchResultsCount = total) }
            }
        }
    }

    operator fun invoke(action: ArtistsListAction) {
        when (action) {
            is ArtistsListAction.Search -> startSearch(action.query)
            is ArtistsListAction.ToggleArtistFavorite -> toggleFavorite(action.artistId, action.isFavorite)
            is ArtistsListAction.OpenReleases -> openReleases(action.artist)
            ArtistsListAction.CloseReleases -> closeReleases()
            ArtistsListAction.ToggleSearchTune -> {}
            ArtistsListAction.Retry -> startSearch(state.value.query)
            ArtistsListAction.LoadingCompleted -> scope.launch {
                _state.update { it.copy(isLoading = false) }
                _listEvents.emit(ArtistsListEvent.ScrollUp)
            }
        }
    }

    private fun openReleases(artist: Artist) {
        _state.update { it.copy(openedArtistId = artist.id, isOpenedArtistFavorite = artist.isFavorite) }
        scope.launch {
            repository.cacheArtist(artist.id)
            hostAction(ArtistsHostAction.ShowReleases(artist.id, artist.name))
        }
    }

    private fun closeReleases() {
        _state.update { it.copy(openedArtistId = null, isOpenedArtistFavorite = false) }
        hostAction(ArtistsHostAction.CloseReleases)
    }

    private fun startSearch(query: String) {
        if (query.isBlank()) return
        hostAction(ArtistsHostAction.CloseReleases)
        _state.update { it.copy(isLoading = true, searchResultsCount = null, query = query) }
        pager.search(query)
    }

    private fun toggleFavorite(artistId: String, isFavorite: Boolean) = scope.launch {
        if (isFavorite) {
            repository.deleteFromFavorites(artistId)
        } else {
            repository.addToFavorite(artistId)
        }
        if (state.value.openedArtistId != null) _state.update {
            it.copy(isOpenedArtistFavorite = !isFavorite)
        }
    }

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
                    if (artist.id in favoriteIds)
                        artist.copy(isFavorite = true) else artist
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