package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

sealed interface ArtistsListEvent {
    data object ScrollUp : ArtistsListEvent
}