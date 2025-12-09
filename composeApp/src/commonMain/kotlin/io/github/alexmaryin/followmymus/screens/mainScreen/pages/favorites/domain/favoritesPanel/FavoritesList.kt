package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.RemoteSyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.SyncRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.FavoritesPanelSlots
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class FavoritesList(
    private val repository: ArtistsRepository,
    private val syncRepository: SyncRepository,
    private val context: ComponentContext,
    private val hostAction: (FavoritesHostAction) -> Unit
) : Page, ComponentContext by context, KoinComponent {

    override val key = "FavoritesList"
    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = FavoritesPanelSlots(
        component = this,
        onRefreshReleases = { hostAction(FavoritesHostAction.RefreshReleases) }
    )

    private val scope = context.coroutineScope()

    private val _state by saveableMutableValue(
        FavoritesListState.serializer(), init = ::FavoritesListState
    )
    val state: Value<FavoritesListState> = _state

    val favoriteArtists = repository.getFavoriteArtists(state.asFlow().map { it.sortingType })
        .onEach { grouped ->
            val count = grouped.values.sumOf { it.size }
            if (state.value.favoritesCount != count)
                _state.update { it.copy(favoritesCount = count) }
        }

    init {
        lifecycle.doOnStart {
            startSync { favoriteArtists.first().isEmpty() }

            scope.launch {
                syncRepository.syncStatus.collect { status ->
                    _state.update { state -> state.copy(isLoading = status == RemoteSyncStatus.Process) }
                }
            }
        }
    }

    private fun startSync(trigger: suspend () -> Boolean = { true }) = scope.launch {
        if (trigger()) syncRepository.syncRemote()
    }

    operator fun invoke(action: FavoritesListAction) {
        when (action) {
            is FavoritesListAction.SelectArtist -> {
                _state.update { it.copy(selectedArtist = action.artistId) }
                hostAction(FavoritesHostAction.ShowReleases(action.artistId, action.artistName))
            }

            is FavoritesListAction.OpenConfirmToRemove -> _state.update {
                it.copy(isRemoveDialogVisible = true, artistToRemove = action.artist)
            }

            FavoritesListAction.DismissRemoveDialog -> _state.update {
                it.copy(isRemoveDialogVisible = false, artistToRemove = null)
            }

            is FavoritesListAction.RemoveFromFavorite -> removeFromFavorite()

            is FavoritesListAction.ChangeSorting -> {
                _state.update { it.copy(sortingType = action.newSort) }
            }

            FavoritesListAction.Refresh -> startSync()

            FavoritesListAction.DeselectArtist -> {
                _state.update { it.copy(selectedArtist = null) }
                hostAction(FavoritesHostAction.CloseReleases)
            }
        }
    }

    private fun removeFromFavorite() {
        state.value.artistToRemove?.let { artist ->
            scope.launch {
                repository.deleteFromFavorites(artist.id)
                _state.update { it.copy(artistToRemove = null, isRemoveDialogVisible = false) }
                hostAction(FavoritesHostAction.OnBack)
            }
        }
    }
}