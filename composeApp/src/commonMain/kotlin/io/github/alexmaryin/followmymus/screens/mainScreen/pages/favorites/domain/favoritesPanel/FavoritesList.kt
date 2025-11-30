package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.FavoritesListTitle
import io.github.alexmaryin.followmymus.screens.utils.sortAbcOrder
import io.github.alexmaryin.followmymus.screens.utils.sortDateCategoryGroups
import io.github.alexmaryin.followmymus.screens.utils.toDateCategory
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent

class FavoritesList(
    private val repository: ArtistsRepository,
    private val context: ComponentContext,
    private val hostAction: (FavoritesHostAction) -> Unit
) : ComponentContext by context, KoinComponent, ScaffoldSlots by DefaultScaffoldSlots {

    private val scope = context.coroutineScope() + SupervisorJob()

    private val _state by saveableMutableValue(
        FavoritesListState.serializer(), init = ::FavoritesListState
    )
    val state: Value<FavoritesListState> = _state
    private val sortingType = MutableStateFlow(state.value.sortingType)

    val favoriteArtists = repository.getFavoriteArtists()
        .combine(sortingType) { artists, sorting ->
            _state.update { it.copy(favoritesCount = artists.size) }
            when (sorting) {
                SortArtists.NONE -> mapOf("" to artists)

                SortArtists.DATE -> artists.groupBy { artist -> artist.createdAt.toDateCategory() }
                    .sortDateCategoryGroups()

                SortArtists.COUNTRY -> artists.groupBy { artist ->
                    artist.country?.toLocalizedResourceName()?.let { getString(it) } ?: "-"
                }

                SortArtists.ABC -> artists.groupBy { artist -> artist.sortName.first().titlecaseChar() }
                    .sortAbcOrder()

                SortArtists.TYPE -> artists.groupBy { artist -> artist.type.name }
            }
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

            is FavoritesListAction.RemoveFromFavorite -> scope.launch { removeFromFavorite() }
        }
    }

    private suspend fun removeFromFavorite() {
        state.value.artistToRemove?.let {
            repository.deleteFromFavorites(it.id)
        }
        _state.update { it.copy(artistToRemove = null, isRemoveDialogVisible = false) }
        hostAction(FavoritesHostAction.CloseReleases)
    }

    override val titleContent = @Composable {
        FavoritesListTitle(selectedSorting = state.value.sortingType) { new ->
            _state.update { it.copy(sortingType = new) }
            sortingType.update { new }
        }
    }
}