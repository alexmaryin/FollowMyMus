package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

data class ArtistReleases(
    val id: String,
    val name: String,
    val resources: List<Resource> = emptyList(),
    val releases: List<Release> = emptyList()
)
