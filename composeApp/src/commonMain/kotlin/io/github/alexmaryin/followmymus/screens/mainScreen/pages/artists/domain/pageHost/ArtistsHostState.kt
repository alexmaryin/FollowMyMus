package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageState

data class ArtistsHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
    val selectedArtistId: String? = null
) : PageState(backVisible)
