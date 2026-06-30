package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list

sealed interface NewReleasesListAction {
    data class SelectRelease(val releaseId: String, val releaseName: String) : NewReleasesListAction
    data class Dismiss(val releaseId: String) : NewReleasesListAction
    data object LoadFromRemote : NewReleasesListAction
    data class OnMediaOpened(val releaseId: String) : NewReleasesListAction
    data object RestoreAllDismissed : NewReleasesListAction
    data object UndoLastDismissal : NewReleasesListAction
    data object RestoreLastMonth : NewReleasesListAction
}
