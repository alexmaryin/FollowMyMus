package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models

import androidx.paging.PagingData

data class ArtistsResult(
    val count: Int,
    val page: PagingData<Artist>
)
