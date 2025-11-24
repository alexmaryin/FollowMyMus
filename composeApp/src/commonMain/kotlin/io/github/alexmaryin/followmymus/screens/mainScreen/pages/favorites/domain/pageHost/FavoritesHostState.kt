package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import kotlinx.serialization.Serializable

@Serializable
data class FavoritesHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val selectedSorting: SortArtists = SortArtists.NONE,
    val backVisible: Boolean = false,
): PageState(backVisible)
