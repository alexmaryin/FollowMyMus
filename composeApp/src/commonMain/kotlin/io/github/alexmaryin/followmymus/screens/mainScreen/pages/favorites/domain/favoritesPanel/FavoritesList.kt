package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.rootNavigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsRepository
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FavoritesList(
    private val context: ComponentContext
) : ComponentContext by context, KoinComponent {

    private val repository by inject<ArtistsRepository>()
    private val scope = context.coroutineScope() + SupervisorJob()

    val favoriteArtists = repository.getFavoriteArtists()

    private val _state by saveableMutableValue(FavoritesListState.serializer(), init = ::FavoritesListState)
    val state: Value<FavoritesListState> = _state

    operator fun invoke(action: FavoriteListAction) {
        when (action) {
            is FavoriteListAction.SelectArtist -> TODO()
            is FavoriteListAction.RemoveFromFavorite -> scope.launch { removeFromFavorite(action.artistId) }
        }
    }

    // TODO confirmation dialog for removing artist from favorites list
    private suspend fun removeFromFavorite(artistId: String) {
        repository.deleteFromFavorites(artistId)
    }
}