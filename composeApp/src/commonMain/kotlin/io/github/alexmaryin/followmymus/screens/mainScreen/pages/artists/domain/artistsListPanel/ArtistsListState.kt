package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import kotlinx.serialization.Serializable

@Serializable
data class ArtistsListState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResultsCount: Int? = null,
    val openedArtistId: String? = null,
    val isOpenedArtistFavorite: Boolean = false
)
