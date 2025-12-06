package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel

import androidx.compose.runtime.Composable
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components.ArtistsSearchBar

class ArtistsPanelSlots(
    private val component: ArtistsList
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent = @Composable { ArtistsSearchBar(component::invoke) }
}