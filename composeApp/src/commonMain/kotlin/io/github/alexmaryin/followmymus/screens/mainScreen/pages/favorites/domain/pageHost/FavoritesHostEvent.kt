package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

sealed interface FavoritesHostEvent {
    data class Error(val message: String) : FavoritesHostEvent
}