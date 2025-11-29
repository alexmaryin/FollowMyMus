package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHostAction
import io.github.alexmaryin.followmymus.screens.utils.sortAbcOrder
import io.github.alexmaryin.followmymus.screens.utils.sortDateCategoryGroups
import io.github.alexmaryin.followmymus.screens.utils.toDateCategory
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FavoritesList(
    private val sortingType: SortArtists,
    private val context: ComponentContext,
    private val hostAction: (FavoritesHostAction) -> Unit
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
    private val detailsRepository by inject<ReleasesRepository>()
    private val scope = context.coroutineScope() + SupervisorJob()

    val favoriteArtists = repository.getFavoriteArtists().map { list ->
        _state.update { it.copy(favoritesCount = list.size) }
        when (sortingType) {
            SortArtists.NONE -> mapOf("" to list)

            SortArtists.DATE -> list.groupBy { artist -> artist.createdAt.toDateCategory() }
                .sortDateCategoryGroups()

            SortArtists.COUNTRY -> list.groupBy { artist ->
                artist.country?.toLocalizedResourceName()?.let { getString(it) } ?: "-"
            }

            SortArtists.ABC -> list.groupBy { artist -> artist.sortName.first().titlecaseChar() }
                .sortAbcOrder()

            SortArtists.TYPE -> list.groupBy { artist -> artist.type.name }
        }
    }

    private val _state by saveableMutableValue(
        FavoritesListState.serializer(),
        init = { FavoritesListState(sortingType = sortingType) }
    )
    val state: Value<FavoritesListState> = _state

    operator fun invoke(action: FavoritesListAction) {
        when (action) {
            is FavoritesListAction.SelectArtist -> {
                _state.update { it.copy(selectedArtist = action.artistId) }
                hostAction(FavoritesHostAction.ShowReleases(action.artistId))
            }

            is FavoritesListAction.OpenConfirmToRemove -> _state.update {
                it.copy(isRemoveDialogVisible = true, artistToRemove = action.artist)
            }

            FavoritesListAction.DismissRemoveDialog -> _state.update {
                it.copy(isRemoveDialogVisible = false, artistToRemove = null)
            }

            is FavoritesListAction.RemoveFromFavorite -> scope.launch { removeFromFavorite() }

            is FavoritesListAction.UpdateDetails -> scope.launch {
                detailsRepository.clearDetails(action.artistId)
                detailsRepository.syncReleases(action.artistId)
            }
        }
    }

    private suspend fun removeFromFavorite() {
        state.value.artistToRemove?.let {
            repository.deleteFromFavorites(it.id)
        }
        _state.update { it.copy(artistToRemove = null, isRemoveDialogVisible = false) }
        hostAction(FavoritesHostAction.CloseReleases)
    }
}