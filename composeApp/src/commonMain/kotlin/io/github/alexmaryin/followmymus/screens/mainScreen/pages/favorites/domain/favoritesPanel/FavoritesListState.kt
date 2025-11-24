package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import kotlinx.serialization.Serializable

@Serializable
data class FavoritesListState(
    val isLoading: Boolean = false,
    val favoritesCount: Int = 0,
    val isSyncing: Boolean = false,
    val isRemoveDialogVisible: Boolean = false,
    val artistToRemove: ArtistToRemove? = null,
    val sortingType: SortArtists
)

@Serializable
data class ArtistToRemove(
    val id: String,
    val name: String
)

fun FavoriteArtist.setToRemove() = ArtistToRemove(id, name)
