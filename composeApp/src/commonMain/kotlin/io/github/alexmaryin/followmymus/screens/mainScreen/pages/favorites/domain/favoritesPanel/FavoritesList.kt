package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.FavoritesListTitle
import io.github.alexmaryin.followmymus.screens.utils.DateCategory
import io.github.alexmaryin.followmymus.screens.utils.sortAbcOrder
import io.github.alexmaryin.followmymus.screens.utils.sortDateCategoryGroups
import io.github.alexmaryin.followmymus.screens.utils.toDateCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class FavoritesList(
    private val repository: ArtistsRepository,
    private val context: ComponentContext,
    private val hostAction: (FavoritesHostAction) -> Unit
) : ComponentContext by context, KoinComponent, ScaffoldSlots by DefaultScaffoldSlots {

    private val scope = context.coroutineScope()

    private val _state by saveableMutableValue(
        FavoritesListState.serializer(), init = ::FavoritesListState
    )
    val state: Value<FavoritesListState> = _state

    val favoriteArtists: Flow<Map<out SortKeyType, List<FavoriteArtist>>> = repository.getFavoriteArtists()
        .onEach { artists ->
            if (state.value.favoritesCount != artists.size)
                _state.update { it.copy(favoritesCount = artists.size) }
        }
        .combine(state.asFlow().map { it.sortingType }.distinctUntilChanged()) { artists, sorting ->
            when (sorting) {
                SortArtists.NONE -> mapOf(SortKeyType.None to artists)

                SortArtists.DATE -> artists.groupBy { artist -> artist.createdAt.toDateCategory() }
                    .sortDateCategoryGroups()

                SortArtists.COUNTRY -> artists.groupBy { artist -> SortKeyType.Country(artist.country) }

                SortArtists.ABC -> artists.groupBy { artist -> artist.sortName.first().titlecaseChar() }
                    .sortAbcOrder()

                SortArtists.TYPE -> artists.groupBy { artist -> SortKeyType.Type(artist.type) }
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

            is FavoritesListAction.RemoveFromFavorite -> removeFromFavorite()

            is FavoritesListAction.ChangeSorting -> {
                _state.update { it.copy(sortingType = action.newSort) }
            }
        }
    }

    private fun removeFromFavorite() {
        state.value.artistToRemove?.let { artist ->
            scope.launch {
                repository.deleteFromFavorites(artist.id)
                _state.update { it.copy(artistToRemove = null, isRemoveDialogVisible = false) }
                hostAction(FavoritesHostAction.CloseReleases)
            }
        }
    }

    override val titleContent = @Composable {
        FavoritesListTitle(selectedSorting = state.value.sortingType) { newSort ->
            invoke(FavoritesListAction.ChangeSorting(newSort))
        }
    }
}

sealed class SortKeyType(val key: String) {
    data object None : SortKeyType("")
    data class Date(val date: DateCategory) : SortKeyType(date.toString())
    data class Country(val country: CountryISO) : SortKeyType(country.country)
    data class Abc(val letter: String) : SortKeyType(letter)
    data class Type(val value: ArtistType) : SortKeyType(value.name)
}