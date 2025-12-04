package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists

sealed interface FavoritesListAction {
    data class SelectArtist(val artistId: String, val artistName: String) : FavoritesListAction
    data class ChangeSorting(val newSort: SortArtists) : FavoritesListAction
    data class OpenConfirmToRemove(val artist: ArtistToRemove) : FavoritesListAction
    data object RemoveFromFavorite : FavoritesListAction
    data object DismissRemoveDialog : FavoritesListAction
    data object Refresh : FavoritesListAction
    data object DeselectArtist : FavoritesListAction
}