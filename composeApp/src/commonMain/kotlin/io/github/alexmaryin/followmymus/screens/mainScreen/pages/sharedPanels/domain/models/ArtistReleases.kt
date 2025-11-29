package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType

data class ArtistReleases(
    val id: String,
    val name: String,
    val resources: Map<String, List<Resource>> = emptyMap(),
    val releases: Map<ReleaseType, List<Release>> = emptyMap()
)
