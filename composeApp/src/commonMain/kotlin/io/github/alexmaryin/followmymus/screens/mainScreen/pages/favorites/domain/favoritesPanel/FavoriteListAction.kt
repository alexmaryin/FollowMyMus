package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

sealed interface FavoriteListAction {
    data class SelectArtist(val artistId: String) : FavoriteListAction
    data class OpenConfirmToRemove(val artist: ArtistToRemove) : FavoriteListAction
    data object DismissRemoveDialog : FavoriteListAction
    data object RemoveFromFavorite : FavoriteListAction
}