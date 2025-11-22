package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.rootNavigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsRepository
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FavoritesList(
    private val context: ComponentContext
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
    private val scope = context.coroutineScope() + SupervisorJob()

    val favoriteArtists = repository.getFavoriteArtists().map { list ->
        _state.update { it.copy(favoritesCount = list.size) }
        println(list)
        list
    }

    private val _state by saveableMutableValue(FavoritesListState.serializer(), init = ::FavoritesListState)
    val state: Value<FavoritesListState> = _state

    operator fun invoke(action: FavoriteListAction) {
        when (action) {
            is FavoriteListAction.SelectArtist -> TODO()

            is FavoriteListAction.OpenConfirmToRemove -> _state.update {
                it.copy(isRemoveDialogVisible = true, artistToRemove = action.artist)
            }

            FavoriteListAction.DismissRemoveDialog -> _state.update {
                it.copy(isRemoveDialogVisible = false, artistToRemove = null)
            }

            is FavoriteListAction.RemoveFromFavorite -> scope.launch { removeFromFavorite() }
        }
    }

    private suspend fun removeFromFavorite() {
        state.value.artistToRemove?.let {
            repository.deleteFromFavorites(it.id)
        }
        _state.update { it.copy(artistToRemove = null, isRemoveDialogVisible = false) }
    }
}