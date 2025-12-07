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
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.FavoritesPanelSlots
import io.github.alexmaryin.followmymus.screens.utils.sortAbcOrder
import io.github.alexmaryin.followmymus.screens.utils.sortCountryOrder
import io.github.alexmaryin.followmymus.screens.utils.sortDateCategoryGroups
import io.github.alexmaryin.followmymus.screens.utils.toDateCategory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class FavoritesList(
    private val repository: ArtistsRepository,
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

    val favoriteArtists: Flow<LinkedHashMap<out SortKeyType, List<FavoriteArtist>>> = repository.getFavoriteArtists()
        .onEach { artists ->
            if (state.value.favoritesCount != artists.size)
                _state.update { it.copy(favoritesCount = artists.size) }
        }
        .combine(state.asFlow().map { it.sortingType }.distinctUntilChanged()) { artists, sorting ->
            val data = when (sorting) {
                SortArtists.NONE -> mapOf(SortKeyType.None to artists)

                SortArtists.DATE -> artists.groupBy { artist -> artist.createdAt.toDateCategory() }
                    .sortDateCategoryGroups()

                SortArtists.COUNTRY -> artists.groupBy { artist -> SortKeyType.Country(artist.country) }
                    .sortCountryOrder()

                SortArtists.ABC -> artists.groupBy { artist -> artist.sortName.first().titlecaseChar() }
                    .sortAbcOrder()

                SortArtists.TYPE -> artists.groupBy { artist -> SortKeyType.Type(artist.type) }
            }
            LinkedHashMap(data)
        }

    init {
        lifecycle.doOnStart {
            startSync { favoriteArtists.first().isEmpty() }

            scope.launch {
                repository.syncStatus.collect { status ->
                    _state.update { state -> state.copy(isLoading = status == RemoteSyncStatus.Process) }
                }
            }
        }
    }

    private fun startSync(trigger: suspend () -> Boolean = { true }) = scope.launch {
        if (trigger()) repository.syncRemote()
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