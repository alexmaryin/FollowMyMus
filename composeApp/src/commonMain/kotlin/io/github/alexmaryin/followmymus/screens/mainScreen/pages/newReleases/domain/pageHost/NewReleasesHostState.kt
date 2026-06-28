package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost

import kotlinx.serialization.Serializable

@Serializable
data class NewReleasesHostState(
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
)
