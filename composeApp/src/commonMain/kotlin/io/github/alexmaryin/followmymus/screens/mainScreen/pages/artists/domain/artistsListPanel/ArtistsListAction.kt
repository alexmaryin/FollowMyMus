package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist

sealed interface ArtistsListAction {
    data class Search(val query: String) : ArtistsListAction
    data class OpenReleases(val artist: Artist) : ArtistsListAction
    data object CloseReleases : ArtistsListAction
    data class ToggleArtistFavorite(val artistId: String, val isFavorite: Boolean) : ArtistsListAction
    data object ToggleSearchTune : ArtistsListAction
    data object Retry : ArtistsListAction
    data object LoadingCompleted : ArtistsListAction
}