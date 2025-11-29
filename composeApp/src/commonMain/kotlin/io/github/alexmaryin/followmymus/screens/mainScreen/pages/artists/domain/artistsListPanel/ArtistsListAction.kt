package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist

sealed interface ArtistsListAction {
    data class Search(val query: String) : ArtistsListAction
    data class SelectArtist(val artist: Artist) : ArtistsListAction
    data class ToggleArtistFavorite(val artist: Artist) : ArtistsListAction
    data object ToggleSearchTune : ArtistsListAction
    data object Retry : ArtistsListAction
    data object LoadingCompleted : ArtistsListAction
}