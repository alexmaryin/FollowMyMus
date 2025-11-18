package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel

import androidx.paging.PagingData
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class ArtistsListState(
    val isLoading: Boolean = false,
    val searchResultsCount: Int? = null,
)
