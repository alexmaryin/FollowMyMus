package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction

class FavoritesPanelSlots(
    private val component: FavoritesList,
    private val onRefreshReleases: () -> Unit
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent = @Composable {
        val state by component.state.subscribeAsState()
        FavoritesListTitle(
            selectedSorting = state.sortingType,
            isRefreshEnabled = state.selectedArtist != null,
            onFilterChange = { newSort ->
                component(FavoritesListAction.ChangeSorting(newSort))
            },
            onRefresh = onRefreshReleases
        )
    }
}