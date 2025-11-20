package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

sealed interface FavoriteListAction {
    data class SelectArtist(val artistId: String) : FavoriteListAction
    data class RemoveFromFavorite(val artistId: String) : FavoriteListAction
}