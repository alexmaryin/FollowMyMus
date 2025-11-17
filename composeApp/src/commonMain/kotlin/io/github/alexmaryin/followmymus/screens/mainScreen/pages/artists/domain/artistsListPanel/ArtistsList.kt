package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import androidx.compose.runtime.Composable
import androidx.paging.cachedIn
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistsSearchBar
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ArtistsList(
    private val context: ComponentContext
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
    private val scope = context.coroutineScope() + SupervisorJob()
    private var lastQuery: String? = null

    init {
        lifecycle.doOnStart {
            scope.launch {
                repository.searchCount.collect { count ->
                    _state.update { it.copy(searchResultsCount = count) }
                }
            }
        }
    }

    private val _state = MutableValue(ArtistsListState())
    val state: Value<ArtistsListState> = _state

    operator fun invoke(action: ArtistsListAction) {
        when (action) {
            is ArtistsListAction.Search -> startSearch(action.query)
            is ArtistsListAction.ToggleArtistFavorite -> scope.launch { toggleFavorite(action.artist) }
            is ArtistsListAction.SelectArtist -> TODO()
            ArtistsListAction.ToggleSearchTune -> TODO()
            ArtistsListAction.Retry -> lastQuery?.let { startSearch(it) }
            ArtistsListAction.LoadingCompleted -> _state.update { it.copy(isLoading = false) }
        }
    }

    private fun startSearch(query: String) {
        if (query.isBlank()) return
        lastQuery = query
        _state.update { it.copy(isLoading = true, searchResultsCount = null) }
        val result = repository.searchArtists(query).cachedIn(scope)
        _state.update { it.copy(artists = result) }
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

}