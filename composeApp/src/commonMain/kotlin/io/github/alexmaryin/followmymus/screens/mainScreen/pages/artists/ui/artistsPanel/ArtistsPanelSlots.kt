package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistFavoriteIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistsSearchBar

class ArtistsPanelSlots(
    private val component: ArtistsList
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent = @Composable { ArtistsSearchBar(component::invoke) }

    override val trailingIcon: @Composable (RowScope.() -> Unit) = {
        val state by component.state.subscribeAsState()
        state.openedArtistId?.let { id ->
            ArtistFavoriteIcon(state.isOpenedArtistFavorite) {
                component(
                    ArtistsListAction.ToggleArtistFavorite(
                        id,
                        state.isOpenedArtistFavorite
                    )
                )
            }
        }
    }
}