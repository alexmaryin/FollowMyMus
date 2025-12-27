package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

data class MediaItem(
    val id: String,
    val position: Int,
    val format: String,
    val title: String,
    val trackCount: Int,
    val tracks: List<Track> = emptyList()
)
