package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list

import kotlinx.serialization.Serializable

@Serializable
data class NewReleasesListState(
    val isLoading: Boolean = false,
)
