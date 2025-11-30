package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

sealed interface FavoritesListAction {
    data class SelectArtist(val artistId: String, val artistName: String) : FavoritesListAction
    data class OpenConfirmToRemove(val artist: ArtistToRemove) : FavoritesListAction
    data object DismissRemoveDialog : FavoritesListAction
    data object RemoveFromFavorite : FavoritesListAction
}