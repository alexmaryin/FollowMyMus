package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list

import kotlinx.serialization.Serializable

@Serializable
data class DismissHistory(
    val dismissedIds: List<String> = emptyList(),
    val restoreWasApplied: Boolean = false,
) {
    val hasDismissals: Boolean get() = dismissedIds.isNotEmpty()
}

@Serializable
data class NewReleasesListState(
    val isLoading: Boolean = false,
    val dismissHistory: DismissHistory = DismissHistory(),
)
